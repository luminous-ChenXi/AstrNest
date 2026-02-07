package com.imgbed.upload;

import com.imgbed.storage.StorageService;
import com.imgbed.upload.dto.UploadResponse;
import com.imgbed.upload.record.UploadRecordService;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/uploads")
@RequiredArgsConstructor
public class UploadController {

  private final UploadService uploadService;
  private final StorageService storageService;
  private final UploadRecordService uploadRecordService;

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasAnyRole('ADMIN','API_CLIENT','USER')")
  public List<UploadResponse> upload(@RequestParam("files") MultipartFile[] files,
      Authentication authentication, HttpServletRequest request) {
    return uploadService.uploadFiles(files, authentication, resolveClientIp(request));
  }

  @GetMapping("/{*objectKey}")
  public ResponseEntity<Resource> fetchObject(@PathVariable("objectKey") String objectKey) {
    Resource resource = storageService.loadAsResource(objectKey);
    uploadRecordService.recordFetch(objectKey);
    MediaType contentType = detectContentType(resource);
    return ResponseEntity.ok()
        .contentType(contentType)
        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=" + resource.getFilename())
        .body(resource);
  }

  private MediaType detectContentType(Resource resource) {
    try {
      Path path = resource.getFile().toPath();
      String probe = Files.probeContentType(path);
      if (probe != null) {
        return MediaType.parseMediaType(probe);
      }
    } catch (IOException | IllegalStateException ignored) {
    }
    return MediaType.parseMediaType(MimeTypeUtils.APPLICATION_OCTET_STREAM_VALUE);
  }

  private String resolveClientIp(HttpServletRequest request) {
    if (request == null) {
      return "unknown";
    }
    String[] headerNames = {"X-Forwarded-For", "X-Real-IP", "CF-Connecting-IP"};
    for (String header : headerNames) {
      String value = request.getHeader(header);
      if (value != null && !value.isBlank()) {
        return value.split(",")[0].trim();
      }
    }
    return request.getRemoteAddr();
  }
}

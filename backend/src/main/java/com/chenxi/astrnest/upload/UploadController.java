package com.chenxi.astrnest.upload;

import com.chenxi.astrnest.storage.StorageService;
import com.chenxi.astrnest.upload.dto.UploadBatchResponse;
import com.chenxi.astrnest.upload.dto.UploadResponse;
import com.chenxi.astrnest.upload.record.UploadRecordService;
import com.chenxi.astrnest.system.SystemConfigService;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
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
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/uploads")
@RequiredArgsConstructor
public class UploadController {

  private final UploadService uploadService;
  private final StorageService storageService;
  private final UploadRecordService uploadRecordService;
  private final SystemConfigService systemConfigService;

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasAnyRole('ADMIN','API_CLIENT','USER')")
  public UploadBatchResponse upload(@RequestParam("files") MultipartFile[] files,
      @RequestParam(value = "tags", required = false) List<String> tags,
      Authentication authentication, HttpServletRequest request) {
    // 检查文件数量限制
    int maxFiles = systemConfigService.currentMaxFilesPerUpload();
    if (files != null && files.length > maxFiles) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "上传图片数量超限，单次最多允许上传 " + maxFiles + " 个文件，请分批上传"
      );
    }
    return uploadService.uploadFilesWithSkip(files, authentication, resolveClientIp(request), tags);
  }

  @GetMapping("/{*objectKey}")
  public ResponseEntity<Resource> fetchObject(@PathVariable("objectKey") String objectKey) {
    Resource resource = storageService.loadAsResource(objectKey);
    uploadRecordService.recordFetch(objectKey);
    MediaType contentType = detectContentType(resource);
    return ResponseEntity.ok()
        .contentType(Objects.requireNonNull(contentType))
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

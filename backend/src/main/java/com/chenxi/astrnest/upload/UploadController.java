package com.chenxi.astrnest.upload;

import com.chenxi.astrnest.storage.StorageService;
import com.chenxi.astrnest.upload.dto.UploadBatchResponse;
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
  private final GuestUploadService guestUploadService;

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public UploadBatchResponse upload(@RequestParam("files") MultipartFile[] files,
      @RequestParam(value = "tags", required = false) List<String> tags,
      @RequestParam(value = "videoCovers", required = false) MultipartFile[] videoCovers,
      @RequestParam(value = "videoCoverMapping", required = false) List<String> videoCoverMapping,
      Authentication authentication, HttpServletRequest request) {
    // 检查是否允许访客上传
    boolean isAuthenticated = authentication != null && authentication.isAuthenticated()
        && !"anonymousUser".equals(authentication.getPrincipal());
    String clientIp = resolveClientIp(request);

    if (!isAuthenticated) {
      // 访客上传权限检查（内部含 guestUploadEnabled 开关判断）
      guestUploadService.checkGuestUploadPermission(clientIp);
    }

    // 检查文件数量限制 - 人性化提示
    int maxFilesPerUpload = systemConfigService.currentMaxFilesPerUpload();
    int totalFiles = (files != null ? files.length : 0) + (videoCovers != null ? videoCovers.length : 0);
    if (totalFiles > maxFilesPerUpload) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "上传图片数量超限，单次最多允许上传 " + maxFilesPerUpload + " 个文件，请分批上传");
    }

    UploadBatchResponse response = uploadService.uploadFilesWithSkip(files, authentication, clientIp, tags,
        videoCovers, videoCoverMapping);

    // 记录访客上传数量
    if (!isAuthenticated && response.uploaded() != null) {
      guestUploadService.recordGuestUpload(clientIp, response.uploaded().size());
    }

    return response;
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

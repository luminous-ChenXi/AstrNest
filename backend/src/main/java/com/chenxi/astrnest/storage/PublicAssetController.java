package com.chenxi.astrnest.storage;

import com.chenxi.astrnest.upload.record.UploadRecordService;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriUtils;

@RestController
@RequestMapping("/api/public/assets")
@RequiredArgsConstructor
public class PublicAssetController {

  private final StorageService storageService;
  private final UploadRecordService uploadRecordService;

  @GetMapping({"", "/", "/**"})
  @SuppressWarnings("null")
  public ResponseEntity<Resource> loadAsset(HttpServletRequest request) {
    String objectKey = extractObjectKey(request);
    if (!StringUtils.hasText(objectKey)) {
      throw new StorageObjectNotFoundException("empty-object-key");
    }
    Resource resource = storageService.loadAsResource(objectKey);
    uploadRecordService.recordFetch(objectKey);
    MediaType mediaType = MediaTypeFactory.getMediaType(resource)
        .orElse(MediaType.APPLICATION_OCTET_STREAM);
    return ResponseEntity.ok()
        .header(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000")
        .contentType(mediaType)
        .body(resource);
  }

  @SuppressWarnings("null")
  private String extractObjectKey(HttpServletRequest request) {
    String requestUri = request.getRequestURI();
    if (!StringUtils.hasText(requestUri)) {
      return null;
    }
    String contextPath = StringUtils.hasText(request.getContextPath()) ? request.getContextPath() : "";
    String prefix = contextPath + "/api/public/assets/";
    if (!requestUri.startsWith(prefix)) {
      return null;
    }
    String trimmed = requestUri.substring(prefix.length());
    if (!StringUtils.hasText(trimmed)) {
      return null;
    }
    return UriUtils.decode(trimmed, StandardCharsets.UTF_8);
  }
}

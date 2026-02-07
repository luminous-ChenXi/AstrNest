package com.chenxi.astrnest.storage;

import com.chenxi.astrnest.upload.record.UploadRecordService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
@RequiredArgsConstructor
public class UploadInvokeCounterFilter extends OncePerRequestFilter {

  private final StorageProperties storageProperties;
  private final UploadRecordService uploadRecordService;

  @Override
  protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain) throws ServletException, IOException {
    try {
      maybeRecordInvoke(request);
    } catch (Exception exception) {
      log.debug("Skip invoke counter update: {}", exception.getMessage());
    }
    filterChain.doFilter(request, response);
  }

  private void maybeRecordInvoke(HttpServletRequest request) {
    if (!"GET".equalsIgnoreCase(request.getMethod())) {
      return;
    }
    String basePath = resolveBasePath(request);
    if (!StringUtils.hasText(basePath)) {
      return;
    }
    String requestUri = request.getRequestURI();
    if (!StringUtils.hasText(requestUri) || !requestUri.startsWith(basePath)) {
      return;
    }
    String objectKey = requestUri.substring(basePath.length());
    while (objectKey.startsWith("/")) {
      objectKey = objectKey.substring(1);
    }
    if (!StringUtils.hasText(objectKey)) {
      return;
    }
    uploadRecordService.recordFetch(objectKey);
  }

  private String resolveBasePath(HttpServletRequest request) {
    String publicBase = storageProperties.getLocal().getPublicBaseUrl();
    if (!StringUtils.hasText(publicBase)) {
      publicBase = "/upload";
    }
    if (publicBase.startsWith("http")) {
      return null;
    }
    String normalized = publicBase.startsWith("/") ? publicBase : "/" + publicBase;
    String contextPath = StringUtils.hasText(request.getContextPath()) ? request.getContextPath() : "";
    return contextPath + normalized;
  }
}

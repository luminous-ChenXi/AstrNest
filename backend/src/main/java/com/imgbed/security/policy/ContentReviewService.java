package com.imgbed.security.policy;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ContentReviewService {

  private final ContentPolicyService contentPolicyService;

  public String evaluate(MultipartFile file) {
    ContentPolicy policy = contentPolicyService.currentPolicy();
    validateMimeType(file);
    boolean requiresManualReview = requiresManualReview(file, policy);
    return requiresManualReview ? "REVIEW_REQUIRED" : "APPROVED";
  }

  private void validateMimeType(MultipartFile file) {
    String contentType = file.getContentType();
    if (contentType == null || !contentType.startsWith("image/")) {
      throw new IllegalArgumentException("仅支持图片文件上传");
    }
  }

  private boolean requiresManualReview(MultipartFile file, ContentPolicy policy) {
    if (!policy.isNsfwDetectionEnabled() && !policy.isViolenceDetectionEnabled()) {
      return false;
    }
    long sizeInMb = file.getSize() / (1024 * 1024);
    return sizeInMb >= policy.getManualReviewThreshold();
  }
}

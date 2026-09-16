package com.chenxi.astrnest.security.policy;

import com.chenxi.astrnest.upload.media.MediaCategory;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ContentReviewService {

  private final ContentPolicyService contentPolicyService;

  public String evaluate(MultipartFile file, MediaCategory category) {
    ContentPolicy policy = contentPolicyService.currentPolicy();
    validateMimeType(file, category);
    boolean requiresManualReview = requiresManualReview(file, policy);
    if (category == MediaCategory.VIDEO) {
      return requiresManualReview ? "VIDEO_REVIEW" : "VIDEO_APPROVED";
    }
    return requiresManualReview ? "REVIEW_REQUIRED" : "APPROVED";
  }

  private void validateMimeType(MultipartFile file, MediaCategory category) {
    String rawContentType = file.getContentType();
    String safeContentType = rawContentType == null ? "" : rawContentType;
    String normalizedContentType;
    if (StringUtils.hasText(safeContentType)) {
      normalizedContentType = safeContentType.trim().toLowerCase(Locale.ROOT);
    } else {
      normalizedContentType = "";
    }
    boolean hasContentType = StringUtils.hasText(normalizedContentType);

    if (category == MediaCategory.IMAGE) {
      if (!hasContentType || !normalizedContentType.startsWith("image/")) {
        throw new IllegalArgumentException("仅支持图片文件上传");
      }
      return;
    }
    if (category == MediaCategory.VIDEO) {
      if (!hasContentType || !normalizedContentType.startsWith("video/")) {
        throw new IllegalArgumentException("仅支持常见短视频格式，如 mp4/webm");
      }
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

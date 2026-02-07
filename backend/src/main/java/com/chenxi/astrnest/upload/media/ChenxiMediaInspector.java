package com.chenxi.astrnest.upload.media;

import com.chenxi.astrnest.storage.StorageContext;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Component
public class ChenxiMediaInspector {

  private static final Set<String> IMAGE_MIME_PREFIXES = Set.of(
      "image/jpeg",
      "image/png",
      "image/webp",
      "image/gif",
      "image/bmp",
      "image/tiff",
      "image/svg+xml"
  );

  private static final Set<String> VIDEO_MIME_PREFIXES = Set.of(
      "video/mp4",
      "video/webm",
      "video/ogg",
      "video/quicktime",
      "video/x-msvideo",
      "video/x-matroska"
  );

  private static final Set<String> IMAGE_EXTENSIONS = Set.of(
      ".jpg",
      ".jpeg",
      ".png",
      ".webp",
      ".gif",
      ".bmp",
      ".tiff",
      ".svg"
  );

  private static final Set<String> VIDEO_EXTENSIONS = Set.of(
      ".mp4",
      ".mov",
      ".qt",
      ".webm",
      ".ogv",
      ".avi",
      ".mkv"
  );

  private static final Set<String> FORBIDDEN_EXTENSIONS = Set.of(
      ".sh",
      ".bat",
      ".cmd",
      ".exe",
      ".msi",
      ".js",
      ".mjs",
      ".cjs",
      ".ts",
      ".php",
      ".py",
      ".rb",
      ".yml",
      ".yaml",
      ".json",
      ".conf",
      ".ini",
      ".log",
      ".sql",
      ".db",
      ".txt"
  );

  public MediaInspection inspect(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "上传文件不可为空");
    }
    String originalName = Optional.ofNullable(file.getOriginalFilename()).orElse("unnamed");
    String extension = extractExtension(originalName);
    enforceSafeExtension(extension);
    String contentType = normalizeContentType(file.getContentType());
    MediaCategory category = resolveCategory(contentType, extension)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "仅支持常见图片或短视频格式"));
    validateExtensionAgainstCategory(extension, category);
    return new MediaInspection(category, contentType, extension);
  }

  public void enforceSizeLimit(MultipartFile file, MediaCategory category, long maxImageBytes, long maxVideoBytes) {
    long size = file.getSize();
    long limit = category == MediaCategory.VIDEO ? maxVideoBytes : maxImageBytes;
    if (size > limit) {
      double limitMb = limit / (1024d * 1024d);
      throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "文件超过上限 " + String.format(Locale.ROOT, "%.1f MB", limitMb));
    }
  }

  public Map<String, String> contextMetadata(MediaCategory category) {
    return Map.of(StorageContext.METADATA_MEDIA_CATEGORY, category.storageSegment());
  }

  private void enforceSafeExtension(String extension) {
    if (!StringUtils.hasText(extension)) {
      return;
    }
    if (FORBIDDEN_EXTENSIONS.contains(extension.toLowerCase(Locale.ROOT))) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "检测到可执行脚本或危险文件，已拦截");
    }
  }

  private Optional<MediaCategory> resolveCategory(String contentType, String extension) {
    if (StringUtils.hasText(contentType)) {
      String lower = contentType.toLowerCase(Locale.ROOT);
      if (IMAGE_MIME_PREFIXES.stream().anyMatch(lower::startsWith)) {
        return Optional.of(MediaCategory.IMAGE);
      }
      if (VIDEO_MIME_PREFIXES.stream().anyMatch(lower::startsWith)) {
        return Optional.of(MediaCategory.VIDEO);
      }
    }
    if (StringUtils.hasText(extension)) {
      String lowerExt = extension.toLowerCase(Locale.ROOT);
      if (IMAGE_EXTENSIONS.contains(lowerExt)) {
        return Optional.of(MediaCategory.IMAGE);
      }
      if (VIDEO_EXTENSIONS.contains(lowerExt)) {
        return Optional.of(MediaCategory.VIDEO);
      }
    }
    return Optional.empty();
  }

  private void validateExtensionAgainstCategory(String extension, MediaCategory category) {
    if (!StringUtils.hasText(extension)) {
      return;
    }
    String lowerExt = extension.toLowerCase(Locale.ROOT);
    if (category == MediaCategory.IMAGE && !IMAGE_EXTENSIONS.contains(lowerExt)) {
      throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "不支持的图片格式");
    }
    if (category == MediaCategory.VIDEO && !VIDEO_EXTENSIONS.contains(lowerExt)) {
      throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "不支持的短视频格式");
    }
  }

  private String extractExtension(String originalName) {
    if (!StringUtils.hasText(originalName)) {
      return "";
    }
    int dot = originalName.lastIndexOf('.');
    if (dot < 0 || dot == originalName.length() - 1) {
      return "";
    }
    return originalName.substring(dot).toLowerCase(Locale.ROOT);
  }

  private String normalizeContentType(String contentType) {
    if (!StringUtils.hasText(contentType)) {
      return "application/octet-stream";
    }
    return contentType.trim().toLowerCase(Locale.ROOT);
  }

  public record MediaInspection(MediaCategory category, String contentType, String extension) {}
}

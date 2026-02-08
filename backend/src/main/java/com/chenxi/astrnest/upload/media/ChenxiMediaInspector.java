package com.chenxi.astrnest.upload.media;

import com.chenxi.astrnest.storage.StorageContext;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
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

  private static final byte[] JPEG = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
  private static final byte[] PNG = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
  private static final byte[] GIF = "GIF8".getBytes(StandardCharsets.US_ASCII);
  private static final byte[] BMP = "BM".getBytes(StandardCharsets.US_ASCII);
  private static final byte[] TIFF_LE = new byte[]{0x49, 0x49, 0x2A, 0x00};
  private static final byte[] TIFF_BE = new byte[]{0x4D, 0x4D, 0x00, 0x2A};
  private static final byte[] MATROSKA = new byte[]{0x1A, 0x45, (byte) 0xDF, (byte) 0xA3};
  private static final byte[] OGG = "OggS".getBytes(StandardCharsets.US_ASCII);
  private static final byte[] RIFF = "RIFF".getBytes(StandardCharsets.US_ASCII);
  private static final byte[] AVI = "AVI ".getBytes(StandardCharsets.US_ASCII);
  private static final byte[] WEBP = "WEBP".getBytes(StandardCharsets.US_ASCII);
  private static final byte[] FTYP = "ftyp".getBytes(StandardCharsets.US_ASCII);

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
    validateFileSignature(file, category);
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

  private void validateFileSignature(MultipartFile file, MediaCategory category) {
    byte[] header = readHeader(file, 64);
    if (header.length == 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "无法读取文件内容");
    }
    boolean allowed = switch (category) {
      case IMAGE -> isImageSignature(header);
      case VIDEO -> isVideoSignature(header);
    };
    if (!allowed) {
      throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "文件头与声明的类型不符或不受支持");
    }
  }

  private boolean isImageSignature(byte[] header) {
    return startsWith(header, JPEG)
        || startsWith(header, PNG)
        || startsWith(header, GIF)
        || startsWith(header, BMP)
        || startsWith(header, TIFF_LE)
        || startsWith(header, TIFF_BE)
        || isWebp(header)
        || looksLikeSvg(header);
  }

  private boolean isVideoSignature(byte[] header) {
    return isMp4OrMov(header)
        || isMatroska(header)
        || isOgg(header)
        || isAvi(header);
  }

  private boolean isWebp(byte[] header) {
    return header.length >= 12 && startsWith(header, RIFF) && startsWithAt(header, WEBP, 8);
  }

  private boolean isMp4OrMov(byte[] header) {
    return header.length >= 12 && startsWithAt(header, FTYP, 4);
  }

  private boolean isMatroska(byte[] header) {
    return startsWith(header, MATROSKA);
  }

  private boolean isOgg(byte[] header) {
    return startsWith(header, OGG);
  }

  private boolean isAvi(byte[] header) {
    return header.length >= 12 && startsWith(header, RIFF) && startsWithAt(header, AVI, 8);
  }

  private boolean looksLikeSvg(byte[] header) {
    String prefix = new String(header, StandardCharsets.UTF_8).trim().toLowerCase(Locale.ROOT);
    return prefix.startsWith("<svg");
  }

  private boolean startsWith(byte[] data, byte[] prefix) {
    if (data.length < prefix.length) {
      return false;
    }
    for (int i = 0; i < prefix.length; i++) {
      if (data[i] != prefix[i]) {
        return false;
      }
    }
    return true;
  }

  private boolean startsWithAt(byte[] data, byte[] prefix, int offset) {
    if (offset < 0 || data.length < offset + prefix.length) {
      return false;
    }
    for (int i = 0; i < prefix.length; i++) {
      if (data[offset + i] != prefix[i]) {
        return false;
      }
    }
    return true;
  }

  private byte[] readHeader(MultipartFile file, int maxBytes) {
    try (InputStream inputStream = file.getInputStream()) {
      return inputStream.readNBytes(maxBytes);
    } catch (IOException exception) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "无法读取上传文件：" + exception.getMessage());
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

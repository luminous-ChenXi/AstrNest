package com.chenxi.astrnest.upload.media;

import com.chenxi.astrnest.storage.StorageContext;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Component
@Slf4j
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
    
    // 获取图片尺寸（仅对图片类型）
    Integer width = null;
    Integer height = null;
    if (category == MediaCategory.IMAGE) {
      ImageDimensions dimensions = extractImageDimensions(file);
      width = dimensions.width();
      height = dimensions.height();
    }
    
    return new MediaInspection(category, contentType, extension, width, height);
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

  public record MediaInspection(MediaCategory category, String contentType, String extension, Integer width, Integer height) {}
  
  public record ImageDimensions(Integer width, Integer height) {}
  
  /**
   * 从图片文件中提取宽高信息
   */
  private ImageDimensions extractImageDimensions(MultipartFile file) {
    try (InputStream is = file.getInputStream()) {
      byte[] header = new byte[24];
      int read = is.read(header);
      if (read < 16) {
        return new ImageDimensions(null, null);
      }
      
      // PNG: 宽度在字节 16-19，高度在字节 20-23（大端序）
      if (matches(header, PNG)) {
        int width = ((header[16] & 0xFF) << 24) | ((header[17] & 0xFF) << 16) | 
                    ((header[18] & 0xFF) << 8) | (header[19] & 0xFF);
        int height = ((header[20] & 0xFF) << 24) | ((header[21] & 0xFF) << 16) | 
                     ((header[22] & 0xFF) << 8) | (header[23] & 0xFF);
        return new ImageDimensions(width, height);
      }
      
      // JPEG: 需要解析 SOF 段
      if (matches(header, JPEG)) {
        return extractJpegDimensions(file);
      }
      
      // GIF: 宽度在字节 6-7（小端序），高度在字节 8-9（小端序）
      if (matches(header, GIF)) {
        int width = (header[6] & 0xFF) | ((header[7] & 0xFF) << 8);
        int height = (header[8] & 0xFF) | ((header[9] & 0xFF) << 8);
        return new ImageDimensions(width, height);
      }
      
      // BMP: 宽度在字节 18-21（小端序），高度在字节 22-25（小端序）
      if (matches(header, BMP)) {
        int width = (header[18] & 0xFF) | ((header[19] & 0xFF) << 8) | 
                    ((header[20] & 0xFF) << 16) | ((header[21] & 0xFF) << 24);
        int height = (header[22] & 0xFF) | ((header[23] & 0xFF) << 8) | 
                     ((header[24] & 0xFF) << 16) | ((header[25] & 0xFF) << 24);
        return new ImageDimensions(width, Math.abs(height)); // BMP 高度可能为负数
      }
      
      // WebP: 需要解析 VP8/VP8L 段
      if (matches(header, RIFF) && header.length >= 12 && matchesAt(header, WEBP, 8)) {
        return extractWebpDimensions(file);
      }
      
    } catch (IOException e) {
      log.warn("无法提取图片尺寸: {}", e.getMessage());
    }
    return new ImageDimensions(null, null);
  }
  
  /**
   * 提取 JPEG 图片尺寸
   */
  private ImageDimensions extractJpegDimensions(MultipartFile file) {
    try (InputStream is = file.getInputStream()) {
      // 跳过 SOI 标记
      is.skip(2);
      
      while (true) {
        int marker = is.read();
        if (marker == -1) break;
        
        // 跳过填充字节
        while (marker == 0xFF) {
          marker = is.read();
        }
        
        if (marker == -1) break;
        
        // SOF0, SOF1, SOF2 (基线, 扩展顺序, 渐进)
        if ((marker >= 0xC0 && marker <= 0xC3) || (marker >= 0xC5 && marker <= 0xC7) || 
            (marker >= 0xC9 && marker <= 0xCB) || (marker >= 0xCD && marker <= 0xCF)) {
          byte[] segment = new byte[7];
          if (is.read(segment) == 7) {
            int height = ((segment[1] & 0xFF) << 8) | (segment[2] & 0xFF);
            int width = ((segment[3] & 0xFF) << 8) | (segment[4] & 0xFF);
            return new ImageDimensions(width, height);
          }
        } else if (marker == 0xD9) { // EOI
          break;
        } else {
          // 跳过其他段
          byte[] lengthBytes = new byte[2];
          if (is.read(lengthBytes) != 2) break;
          int length = ((lengthBytes[0] & 0xFF) << 8) | (lengthBytes[1] & 0xFF);
          if (length > 2) {
            is.skip(length - 2);
          }
        }
      }
    } catch (IOException e) {
      log.warn("无法提取 JPEG 尺寸: {}", e.getMessage());
    }
    return new ImageDimensions(null, null);
  }
  
  /**
   * 提取 WebP 图片尺寸
   */
  private ImageDimensions extractWebpDimensions(MultipartFile file) {
    try (InputStream is = file.getInputStream()) {
      // 跳过 RIFF 头和 WEBP 标识
      is.skip(12);
      
      byte[] chunkHeader = new byte[8];
      while (is.read(chunkHeader) == 8) {
        String chunkType = new String(chunkHeader, 0, 4, StandardCharsets.US_ASCII);
        int chunkSize = (chunkHeader[4] & 0xFF) | ((chunkHeader[5] & 0xFF) << 8) |
                        ((chunkHeader[6] & 0xFF) << 16) | ((chunkHeader[7] & 0xFF) << 24);
        
        if ("VP8 ".equals(chunkType)) {
          // 有损 WebP
          byte[] vp8Data = new byte[10];
          if (is.read(vp8Data) == 10) {
            int width = ((vp8Data[6] & 0xFF) | ((vp8Data[7] & 0xFF) << 8)) & 0x3FFF;
            int height = ((vp8Data[8] & 0xFF) | ((vp8Data[9] & 0xFF) << 8)) & 0x3FFF;
            return new ImageDimensions(width, height);
          }
        } else if ("VP8L".equals(chunkType)) {
          // 无损 WebP
          byte[] vp8lData = new byte[5];
          if (is.read(vp8lData) == 5) {
            int bits = (vp8lData[1] & 0xFF) | ((vp8lData[2] & 0xFF) << 8) | 
                       ((vp8lData[3] & 0xFF) << 16) | ((vp8lData[4] & 0xFF) << 24);
            int width = (bits & 0x3FFF) + 1;
            int height = ((bits >> 14) & 0x3FFF) + 1;
            return new ImageDimensions(width, height);
          }
        } else if ("VP8X".equals(chunkType)) {
          // 扩展 WebP
          byte[] vp8xData = new byte[10];
          if (is.read(vp8xData) == 10) {
            int width = ((vp8xData[4] & 0xFF) | ((vp8xData[5] & 0xFF) << 8) | 
                        ((vp8xData[6] & 0xFF) << 16)) + 1;
            int height = ((vp8xData[7] & 0xFF) | ((vp8xData[8] & 0xFF) << 8) | 
                         ((vp8xData[9] & 0xFF) << 16)) + 1;
            return new ImageDimensions(width, height);
          }
        } else {
          // 跳过其他 chunk
          if (chunkSize > 0) {
            is.skip(chunkSize + (chunkSize % 2)); // 包括填充字节
          }
        }
      }
    } catch (IOException e) {
      log.warn("无法提取 WebP 尺寸: {}", e.getMessage());
    }
    return new ImageDimensions(null, null);
  }
  
  private boolean matchesAt(byte[] data, byte[] pattern, int offset) {
    if (data.length < offset + pattern.length) return false;
    for (int i = 0; i < pattern.length; i++) {
      if (data[offset + i] != pattern[i]) return false;
    }
    return true;
  }
  
  private boolean matches(byte[] data, byte[] pattern) {
    return matchesAt(data, pattern, 0);
  }
}

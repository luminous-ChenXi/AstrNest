package com.chenxi.astrnest.upload.media;

import com.chenxi.astrnest.storage.PublicAssetUrlResolver;
import com.chenxi.astrnest.storage.StorageProperties;
import com.chenxi.astrnest.storage.StorageService;
import com.chenxi.astrnest.storage.StoredObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoThumbnailService {

  private final StorageProperties storageProperties;
  private final PublicAssetUrlResolver publicAssetUrlResolver;
  private final StorageService storageService;
  private final VideoThumbnailProperties properties;

  public ThumbnailResult generateThumbnail(StoredObject storedObject) {
    if (!properties.isEnabled() || storedObject == null) {
      return null;
    }
    if (shouldSkipByLoad()) {
      log.warn("当前系统负载较高，跳过缩略图生成以保护服务。load={}", getSystemLoadAverage());
      return null;
    }
    Path tempSource = null;
    try {
      Path source = resolveSourcePath(storedObject);
      if (source == null) {
        tempSource = downloadToTemp(storedObject);
        source = tempSource;
      }
      if (source == null) {
        log.debug("无法定位或下载视频源文件，跳过缩略图生成。objectKey={}", storedObject.objectKey());
        return null;
      }
      ThumbnailLocation location = buildLocation(storedObject);
      Files.createDirectories(location.directory());
      Path tempFile = Files.createTempFile("astrnest-thumb", ".jpg");
      boolean success = runFfmpeg(source, tempFile);
      if (!success) {
        Files.deleteIfExists(tempFile);
        return null;
      }
      Files.move(tempFile, location.absolutePath(), StandardCopyOption.REPLACE_EXISTING);
      String publicUrl = publicAssetUrlResolver.buildLocalPublicUrl(location.relativeKey());
      return new ThumbnailResult(publicUrl, location.relativeKey());
    } catch (IOException ex) {
      log.warn("生成视频缩略图失败，objectKey={}", storedObject.objectKey(), ex);
      return null;
    } finally {
      cleanupTemp(tempSource);
    }
  }

  private boolean shouldSkipByLoad() {
    double maxLoad = properties.getMaxLoadAverage();
    if (maxLoad <= 0) {
      return false;
    }
    double current = getSystemLoadAverage();
    return current > 0 && current >= maxLoad;
  }

  private double getSystemLoadAverage() {
    return java.lang.management.ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage();
  }

  private Path resolveSourcePath(StoredObject storedObject) {
    if (storedObject == null || !StringUtils.hasText(storedObject.absolutePath())) {
      return null;
    }
    Path candidate = Paths.get(storedObject.absolutePath());
    if (Files.exists(candidate)) {
      return candidate;
    }
    return null;
  }

  private Path downloadToTemp(StoredObject storedObject) {
    try {
      Resource resource = storageService.loadAsResource(storedObject.objectKey(), storedObject.providerKey());
      if (resource == null || !resource.exists()) {
        return null;
      }
      String suffix = determineExtension(storedObject.storedFileName());
      Path temp = Files.createTempFile("astrnest-video-src", suffix);
      try (InputStream inputStream = resource.getInputStream()) {
        Files.copy(inputStream, temp, StandardCopyOption.REPLACE_EXISTING);
      }
      return temp;
    } catch (Exception exception) {
      log.warn("下载视频源失败，objectKey={}", storedObject.objectKey(), exception);
      return null;
    }
  }

  private boolean runFfmpeg(Path source, Path output) {
    ProcessBuilder builder = new ProcessBuilder(
        properties.getFfmpegPath(),
        "-y",
        "-ss",
        captureTimestamp(),
        "-i",
        source.toAbsolutePath().toString(),
        "-vframes",
        "1",
        "-q:v",
        String.valueOf(Math.max(1, properties.getQuality())),
        output.toAbsolutePath().toString()
    );
    builder.redirectErrorStream(true);
    try {
      Process process = builder.start();
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
        while (reader.readLine() != null) {
          // 消费输出，避免阻塞
        }
      }
      boolean finished = process.waitFor(Math.max(1, properties.getTimeoutSeconds()), TimeUnit.SECONDS);
      if (!finished) {
        process.destroyForcibly();
        log.warn("ffmpeg 生成缩略图超时");
        return false;
      }
      if (process.exitValue() != 0) {
        log.warn("ffmpeg 生成缩略图失败，退出码 {}", process.exitValue());
        return false;
      }
      return true;
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      log.warn("执行 ffmpeg 被中断: {}", ex.getMessage());
      return false;
    } catch (IOException ex) {
      log.warn("执行 ffmpeg 失败: {}", ex.getMessage());
      return false;
    }
  }

  private ThumbnailLocation buildLocation(StoredObject storedObject) {
    Path root = storageProperties.getLocal().resolvedRoot();
    String coverSegment = StringUtils.hasText(properties.getCoverDirectory())
        ? properties.getCoverDirectory().trim()
        : "cover";
    Path directory = root
        .resolve(coverSegment)
        .resolve(currentYear())
        .resolve(currentMonth());
    String fallbackName = StringUtils.hasText(storedObject.storedFileName())
        ? storedObject.storedFileName()
        : storedObject.objectKey();
    String baseName = stripExtension(fallbackName);
    Path destination = directory.resolve(baseName + "_cover.jpg");
    if (Files.exists(destination)) {
      destination = directory.resolve(baseName + "_cover_" + System.currentTimeMillis() + ".jpg");
    }
    String relativeKey = root.relativize(destination).toString().replace('\\', '/');
    return new ThumbnailLocation(directory, destination, relativeKey);
  }

  private String captureTimestamp() {
    Duration offset = properties.getCaptureOffset();
    if (offset == null || offset.isNegative()) {
      offset = Duration.ZERO;
    }
    long totalMillis = offset.toMillis();
    long hours = totalMillis / 3_600_000;
    long minutes = (totalMillis % 3_600_000) / 60_000;
    long seconds = (totalMillis % 60_000) / 1000;
    long millis = totalMillis % 1000;
    return String.format(Locale.ROOT, "%02d:%02d:%02d.%03d", hours, minutes, seconds, millis);
  }

  private String currentYear() {
    return String.format(Locale.ROOT, "%04d", ZonedDateTime.ofInstant(Instant.now(), ZoneId.systemDefault()).getYear());
  }

  private String currentMonth() {
    return String.format(Locale.ROOT, "%02d", ZonedDateTime.ofInstant(Instant.now(), ZoneId.systemDefault()).getMonthValue());
  }

  private String stripExtension(String name) {
    if (!StringUtils.hasText(name)) {
      return "video";
    }
    String sanitized = name.replace('\\', '_').replace('/', '_').trim();
    int dot = sanitized.lastIndexOf('.');
    if (dot < 0) {
      return sanitized;
    }
    return sanitized.substring(0, dot);
  }

  private String determineExtension(String name) {
    if (!StringUtils.hasText(name)) {
      return ".mp4";
    }
    String trimmed = name.trim();
    int dot = trimmed.lastIndexOf('.');
    if (dot >= 0 && dot < trimmed.length() - 1) {
      return trimmed.substring(dot);
    }
    return ".mp4";
  }

  private void cleanupTemp(Path path) {
    if (path == null) {
      return;
    }
    try {
      Files.deleteIfExists(path);
    } catch (IOException ex) {
      log.debug("删除临时文件失败: {}", ex.getMessage());
    }
  }

  public record ThumbnailResult(String publicUrl, String storageRelativeKey) {}

  private record ThumbnailLocation(Path directory, Path absolutePath, String relativeKey) {}
}

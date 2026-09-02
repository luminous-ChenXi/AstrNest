package com.chenxi.astrnest.storage;

import com.chenxi.astrnest.storage.handler.MediaMeta;
import com.chenxi.astrnest.storage.handler.StorageHandler;
import com.chenxi.astrnest.storage.handler.StorageListRequest;
import com.chenxi.astrnest.storage.handler.StorageListResult;
import com.chenxi.astrnest.storage.handler.StorageObjectSummary;
import com.chenxi.astrnest.storage.handler.StorageTokenRequest;
import com.chenxi.astrnest.storage.handler.StorageTokenResponse;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Component
@RequiredArgsConstructor
public class LocalStorageHandler implements StorageHandler {

  private static final SecureRandom RANDOM = new SecureRandom();
  private static final int CLIPBOARD_RANDOM_DIGITS = 8;

  private final StorageProperties properties;
  private final PublicAssetUrlResolver assetUrlResolver;

  @PostConstruct
  void init() {
    Path root = properties.getLocal().resolvedRoot();
    try {
      Files.createDirectories(root);
      Files.createDirectories(root.resolve("picture"));
      Files.createDirectories(root.resolve("video"));
      log.info("本地存储目录初始化成功: {}", root);
    } catch (IOException ex) {
      log.error("无法创建本地存储目录 '{}'. 请检查权限或配置 'astrnest.storage.local.root' 指向可写目录。", root, ex);
      // 不抛出异常，允许应用启动，但上传功能会失败
    }
  }

  @Override
  public StorageStrategy strategy() {
    return StorageStrategy.LOCAL;
  }

  @Override
  public StoredObject put(MultipartFile file, StorageContext context) {
    ZonedDateTime now = ZonedDateTime.ofInstant(Instant.now(), ZoneId.systemDefault());
    String yearSegment = String.format("%04d", now.getYear());
    String monthSegment = String.format("%02d", now.getMonthValue());
    String mediaSegment = resolveMediaSegment(context);
    Path datedDirectory = properties.getLocal().resolvedRoot()
        .resolve(mediaSegment)
        .resolve(yearSegment)
        .resolve(monthSegment);

    try {
      Files.createDirectories(datedDirectory);
      String preferredName = resolveFileName(file);
      Path destination = resolveDestination(datedDirectory, preferredName);
      Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);

      String storedFileName = destination.getFileName().toString();
      String objectKey = mediaSegment + "/" + yearSegment + "/" + monthSegment + "/" + storedFileName;
      String publicUrl = buildPublicUrl(objectKey);

      log.info("Stored file {} as {}", preferredName, destination);
      return new StoredObject(objectKey, storedFileName, publicUrl, file.getSize(), destination.toAbsolutePath().toString(), StorageStrategy.LOCAL.name());
    } catch (IOException ex) {
      throw new StorageWriteException("Failed to store file", ex);
    }
  }

  @Override
  public void delete(String objectKey) {
    Path root = properties.getLocal().resolvedRoot();
    Path target = root.resolve(objectKey);
    try {
      Files.deleteIfExists(target);
    } catch (IOException ex) {
      log.warn("Failed to delete local object {}", objectKey, ex);
    }
  }

  @Override
  public StorageListResult list(StorageListRequest request) {
    List<StorageObjectSummary> results = new ArrayList<>();
    Path root = properties.getLocal().resolvedRoot();
    Path prefixPath = StringUtils.hasText(request.prefix()) ? root.resolve(request.prefix()) : root;
    if (!Files.exists(prefixPath)) {
      return new StorageListResult(List.of(), null);
    }
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(prefixPath)) {
      for (Path path : stream) {
        if (!Files.isRegularFile(path)) {
          continue;
        }
        results.add(new StorageObjectSummary(
            root.relativize(path).toString().replace('\\', '/'),
            Files.size(path),
            Files.getLastModifiedTime(path).toInstant()
        ));
        if (results.size() >= request.limit()) {
          break;
        }
      }
    } catch (IOException ex) {
      log.warn("Failed to list local storage", ex);
    }
    return new StorageListResult(results, null);
  }

  @Override
  public String source(String objectKey) {
    return buildPublicUrl(objectKey);
  }

  @Override
  public StorageTokenResponse token(StorageTokenRequest request) {
    throw new UnsupportedOperationException("本地存储无需上传凭证");
  }

  @Override
  public MediaMeta mediaMeta(String objectKey) {
    Path root = properties.getLocal().resolvedRoot();
    Path file = root.resolve(objectKey);
    if (!Files.exists(file)) {
      return new MediaMeta(0, 0, 0, null);
    }
    try {
      return new MediaMeta(Files.size(file), 0, 0, Files.probeContentType(file));
    } catch (IOException ex) {
      return new MediaMeta(0, 0, 0, null);
    }
  }

  @Override
  @SuppressWarnings("null")
  public Resource load(String objectKey) {
    Path root = properties.getLocal().resolvedRoot();
    Path file = root.resolve(objectKey);
    if (!Files.exists(file)) {
      throw new StorageObjectNotFoundException(objectKey);
    }
    return new PathResource(file);
  }

  private String resolveFileName(MultipartFile file) {
    String original = sanitizeFileName(file.getOriginalFilename());
    if (!StringUtils.hasText(original) || "blob".equalsIgnoreCase(original)) {
      return generateClipboardName(file.getContentType());
    }
    return original;
  }

  private String sanitizeFileName(String fileName) {
    if (!StringUtils.hasText(fileName)) {
      return "";
    }
    String cleaned = Paths.get(fileName).getFileName().toString();
    return cleaned.replace("\\", "_").replace("/", "_").trim();
  }

  private String generateClipboardName(String contentType) {
    return "luminouscx" + randomDigits(CLIPBOARD_RANDOM_DIGITS) + determineExtension(contentType);
  }

  private String determineExtension(String contentType) {
    if (StringUtils.hasText(contentType) && contentType.contains("/")) {
      String subtype = contentType.substring(contentType.indexOf('/') + 1).trim();
      if (StringUtils.hasText(subtype)) {
        return "." + subtype;
      }
    }
    return ".png";
  }

  private String randomDigits(int length) {
    StringBuilder builder = new StringBuilder(length);
    for (int i = 0; i < length; i++) {
      builder.append(RANDOM.nextInt(10));
    }
    return builder.toString();
  }

  private Path resolveDestination(Path directory, String preferredName) throws IOException {
    Path candidate = directory.resolve(Objects.requireNonNull(preferredName));
    if (!Files.exists(candidate)) {
      return candidate;
    }
    String baseName = preferredName;
    String extension = "";
    int dotIndex = preferredName.lastIndexOf('.');
    if (dotIndex >= 0) {
      baseName = preferredName.substring(0, dotIndex);
      extension = preferredName.substring(dotIndex);
    }
    String uniqueName = baseName + "_" + System.currentTimeMillis() + extension;
    return directory.resolve(uniqueName);
  }

  private String buildPublicUrl(String objectKey) {
    return assetUrlResolver.buildLocalPublicUrl(objectKey);
  }

  private String resolveMediaSegment(StorageContext context) {
    if (context == null) {
      return "picture";
    }
    String value = context.safeMetadata().get(StorageContext.METADATA_MEDIA_CATEGORY);
    if (!StringUtils.hasText(value)) {
      return "picture";
    }
    return value.trim().toLowerCase();
  }
}

package com.chenxi.astrnest.storage;

import com.chenxi.astrnest.storage.handler.StorageHandler;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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

  /**
   * 存储文件名统一使用「日期目录 + 随机文件名」（StorageObjectKeys）：不再保留用户原始文件名，
   * 避免并发同名覆盖（REPLACE_EXISTING 竞态）与直链可枚举；原始文件名保存在 upload_records。
   */
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
      String storedFileName = StorageObjectKeys.randomFileName(file);
      Path destination = datedDirectory.resolve(storedFileName);
      Files.copy(file.getInputStream(), destination);

      String objectKey = mediaSegment + "/" + yearSegment + "/" + monthSegment + "/" + storedFileName;
      String publicUrl = buildPublicUrl(objectKey);

      log.info("Stored file as {} ({})", destination, file.getOriginalFilename());
      return new StoredObject(objectKey, storedFileName, publicUrl, file.getSize(), destination.toAbsolutePath().toString(), StorageStrategy.LOCAL.name());
    } catch (IOException ex) {
      throw new StorageWriteException("Failed to store file", ex);
    }
  }

  @Override
  public void delete(String objectKey) {
    Path root = properties.getLocal().resolvedRoot();
    Path target = StoragePathGuard.assertInsideRoot(root, objectKey);
    try {
      Files.deleteIfExists(target);
    } catch (IOException ex) {
      log.warn("Failed to delete local object {}", objectKey, ex);
    }
  }

  @Override
  @SuppressWarnings("null")
  public Resource load(String objectKey) {
    Path root = properties.getLocal().resolvedRoot();
    Path file = StoragePathGuard.assertInsideRoot(root, objectKey);
    if (!Files.exists(file)) {
      throw new StorageObjectNotFoundException(objectKey);
    }
    return new PathResource(file);
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

package com.chenxi.astrnest.storage.upyun;

import com.chenxi.astrnest.storage.StorageContext;
import com.chenxi.astrnest.storage.StorageProperties;
import com.chenxi.astrnest.storage.StorageStrategy;
import com.chenxi.astrnest.storage.StoredObject;
import com.chenxi.astrnest.storage.StorageObjectNotFoundException;
import com.chenxi.astrnest.storage.StorageWriteException;
import com.chenxi.astrnest.storage.handler.StorageHandler;
import com.upyun.RestManager;
import com.upyun.UpException;
import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Component
@RequiredArgsConstructor
public class UpyunStorageHandler implements StorageHandler {

  private static final int DEFAULT_TIMEOUT_SECONDS = 60;

  private final StorageProperties properties;
  private RestManager restManager;

  @Override
  public StorageStrategy strategy() {
    return StorageStrategy.UPYUN_USS;
  }

  @Override
  public StoredObject put(MultipartFile file, StorageContext context) {
    StorageProperties.Upyun config = config();
    ensureEnabled(config);
    String objectKey = buildObjectKey(file);
    Map<String, String> headers = new HashMap<>();
    headers.put(RestManager.PARAMS.MAKE_DIR.getValue(), "true");
    if (StringUtils.hasText(file.getContentType())) {
      headers.put(RestManager.PARAMS.CONTENT_TYPE.getValue(), file.getContentType());
    }
    try (Response response = manager().writeFile("/" + objectKey, file.getInputStream(), headers)) {
      if (!response.isSuccessful()) {
        throw new StorageWriteException("上传到又拍云失败: " + response.code() + " " + response.message());
      }
      return new StoredObject(
          objectKey,
          extractFileName(objectKey),
          buildPublicUrl(objectKey),
          file.getSize(),
          "/" + objectKey,
          strategy().name()
      );
    } catch (IOException | UpException ex) {
      throw new StorageWriteException("上传到又拍云失败", ex);
    }
  }

  @Override
  public void delete(String objectKey) {
    StorageProperties.Upyun config = config();
    ensureEnabled(config);
    CompletableFuture.runAsync(() -> performDelete(objectKey));
  }

  private void performDelete(String objectKey) {
    try (Response response = manager().deleteFile("/" + sanitizeKey(objectKey), null)) {
      if (response == null || !response.isSuccessful()) {
        log.warn("异步删除又拍云对象 {} 失败: {}", objectKey, response != null ? response.message() : "response null");
      }
    } catch (IOException | UpException ex) {
      log.warn("异步删除又拍云对象 {} 异常", objectKey, ex);
    }
  }

  @Override
  public Resource load(String objectKey) {
    StorageProperties.Upyun config = config();
    ensureEnabled(config);
    try (Response response = manager().readFile("/" + sanitizeKey(objectKey))) {
      if (response == null || !response.isSuccessful() || response.body() == null) {
        throw new StorageObjectNotFoundException(objectKey);
      }
      byte[] data = Objects.requireNonNull(response.body().bytes(), "又拍云响应体为空");
      return new ByteArrayResource(data);
    } catch (IOException | UpException ex) {
      throw new StorageObjectNotFoundException(objectKey);
    }
  }

  private StorageProperties.Upyun config() {
    return properties.getUpyun();
  }

  private void ensureEnabled(StorageProperties.Upyun config) {
    if (config == null || !config.isEnabled()) {
      throw new IllegalStateException("又拍云存储未启用");
    }
    if (!StringUtils.hasText(config.getBucket())
        || !StringUtils.hasText(config.getOperator())
        || !StringUtils.hasText(config.getPassword())) {
      throw new IllegalStateException("请完整配置又拍云 bucket/operator/password");
    }
  }

  private RestManager manager() {
    if (restManager == null) {
      StorageProperties.Upyun config = config();
      restManager = new RestManager(config.getBucket(), config.getOperator(), config.getPassword());
      restManager.setTimeout(DEFAULT_TIMEOUT_SECONDS);
      if (StringUtils.hasText(config.getEndpoint())) {
        restManager.setApiDomain(config.getEndpoint());
      }
    }
    return restManager;
  }

  private String buildObjectKey(MultipartFile file) {
    ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
    String year = String.format("%04d", now.getYear());
    String month = String.format("%02d", now.getMonthValue());
    String fileName = resolveFileName(file);
    return year + "/" + month + "/" + fileName;
  }

  private String resolveFileName(MultipartFile file) {
    String original = sanitizeFileName(file.getOriginalFilename());
    if (!StringUtils.hasText(original) || "blob".equalsIgnoreCase(original)) {
      return "luminouscx" + System.currentTimeMillis() + extensionFromContentType(file.getContentType());
    }
    return original;
  }

  private String sanitizeFileName(String fileName) {
    if (!StringUtils.hasText(fileName)) {
      return "";
    }
    String sanitized = fileName.replace("\\", "_").replace("/", "_").trim();
    return sanitized.isEmpty() ? "unnamed" : sanitized;
  }

  private String extensionFromContentType(String contentType) {
    if (StringUtils.hasText(contentType) && contentType.contains("/")) {
      String subtype = contentType.substring(contentType.indexOf('/') + 1).trim();
      if (StringUtils.hasText(subtype)) {
        return "." + subtype;
      }
    }
    return ".png";
  }

  private String extractFileName(String objectKey) {
    if (!StringUtils.hasText(objectKey)) {
      return "unnamed";
    }
    int index = objectKey.lastIndexOf('/');
    return index >= 0 ? objectKey.substring(index + 1) : objectKey;
  }

  private String sanitizeKey(String key) {
    if (!StringUtils.hasText(key)) {
      return "";
    }
    return key.startsWith("/") ? key.substring(1) : key;
  }

  private String buildPublicUrl(String objectKey) {
    StorageProperties.Upyun config = config();
    String sanitized = sanitizeKey(objectKey);
    if (StringUtils.hasText(config.getCdnHost())) {
      return trimTrailingSlash(config.getCdnHost()) + "/" + sanitized;
    }
    return "https://" + config.getBucket() + ".b0.upaiyun.com/" + sanitized;
  }

  private String trimTrailingSlash(String value) {
    if (!StringUtils.hasText(value)) {
      return "";
    }
    String result = value.trim();
    while (result.endsWith("/")) {
      result = result.substring(0, result.length() - 1);
    }
    return result;
  }
}

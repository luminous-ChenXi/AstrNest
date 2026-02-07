package com.imgbed.storage.ali;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.GetObjectRequest;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;
import com.imgbed.storage.StorageContext;
import com.imgbed.storage.StorageProperties;
import com.imgbed.storage.StorageStrategy;
import com.imgbed.storage.StoredObject;
import com.imgbed.storage.StorageObjectNotFoundException;
import com.imgbed.storage.StorageWriteException;
import com.imgbed.storage.handler.MediaMeta;
import com.imgbed.storage.handler.StorageHandler;
import com.imgbed.storage.handler.StorageListRequest;
import com.imgbed.storage.handler.StorageListResult;
import com.imgbed.storage.handler.StorageObjectSummary;
import com.imgbed.storage.handler.StorageTokenRequest;
import com.imgbed.storage.handler.StorageTokenResponse;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Component
@RequiredArgsConstructor
public class AliyunOssStorageHandler implements StorageHandler {

  private final StorageProperties properties;
  private OSS client;

  @Override
  public StorageStrategy strategy() {
    return StorageStrategy.ALIYUN_OSS;
  }

  @Override
  public StoredObject put(MultipartFile file, StorageContext context) {
    StorageProperties.Oss config = properties.getOss();
    ensureEnabled(config);
    String objectKey = buildObjectKey(file);
    try (InputStream stream = file.getInputStream()) {
      PutObjectRequest request = new PutObjectRequest(config.getBucket(), objectKey, stream);
      oss().putObject(request);
      return new StoredObject(
          objectKey,
          extractFileName(objectKey),
          buildPublicUrl(objectKey),
          file.getSize(),
          objectKey,
          StorageStrategy.ALIYUN_OSS.name()
      );
    } catch (IOException ex) {
      throw new StorageWriteException("上传到 OSS 失败", ex);
    }
  }

  @Override
  public void delete(String objectKey) {
    StorageProperties.Oss config = properties.getOss();
    ensureEnabled(config);
    oss().deleteObject(config.getBucket(), objectKey);
  }

  @Override
  public StorageListResult list(StorageListRequest request) {
    StorageProperties.Oss config = properties.getOss();
    ensureEnabled(config);
    var listing = oss().listObjects(config.getBucket(), request.prefix());
    List<StorageObjectSummary> summaries = new ArrayList<>();
    listing.getObjectSummaries().stream().limit(request.limit()).forEach(summary ->
        summaries.add(new StorageObjectSummary(summary.getKey(), summary.getSize(), summary.getLastModified().toInstant()))
    );
    return new StorageListResult(summaries, listing.getNextMarker());
  }

  @Override
  public String source(String objectKey) {
    return buildPublicUrl(objectKey);
  }

  @Override
  public StorageTokenResponse token(StorageTokenRequest request) {
    throw new UnsupportedOperationException("暂未实现 OSS 直传凭证");
  }

  @Override
  public MediaMeta mediaMeta(String objectKey) {
    StorageProperties.Oss config = properties.getOss();
    ensureEnabled(config);
    ObjectMetadata metadata = oss().getObjectMetadata(config.getBucket(), objectKey);
    return new MediaMeta(metadata.getContentLength(), 0, 0, metadata.getContentType());
  }

  @Override
  public Resource load(String objectKey) {
    StorageProperties.Oss config = properties.getOss();
    ensureEnabled(config);
    OSSObject object = oss().getObject(new GetObjectRequest(config.getBucket(), objectKey));
    if (object == null || object.getObjectContent() == null) {
      throw new StorageObjectNotFoundException(objectKey);
    }
    return new InputStreamResource(Objects.requireNonNull(object.getObjectContent()));
  }

  private OSS oss() {
    if (client == null) {
      StorageProperties.Oss config = properties.getOss();
      ensureEnabled(config);
      client = new OSSClientBuilder().build(config.getEndpoint(), config.getAccessKey(), config.getSecretKey());
    }
    return client;
  }

  private void ensureEnabled(StorageProperties.Oss config) {
    if (config == null || !config.isEnabled()) {
      throw new IllegalStateException("Aliyun OSS 未启用");
    }
  }

  private String buildObjectKey(MultipartFile file) {
    ZonedDateTime now = ZonedDateTime.ofInstant(Instant.now(), ZoneId.systemDefault());
    String year = String.format("%04d", now.getYear());
    String month = String.format("%02d", now.getMonthValue());
    String sanitized = resolveFileName(file);
    return year + "/" + month + "/" + sanitized;
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
    return Paths.get(fileName).getFileName().toString().replace("\\", "_").replace("/", "_").trim();
  }

  private String generateClipboardName(String contentType) {
    String subtype = "png";
    if (StringUtils.hasText(contentType) && contentType.contains("/")) {
      String tail = contentType.substring(contentType.indexOf('/') + 1).trim();
      if (StringUtils.hasText(tail)) {
        subtype = tail;
      }
    }
    return "luminouscx" + System.currentTimeMillis() + "." + subtype;
  }

  private String buildPublicUrl(String objectKey) {
    StorageProperties.Oss config = properties.getOss();
    if (StringUtils.hasText(config.getCdnHost())) {
      return trimSlash(config.getCdnHost()) + "/" + objectKey;
    }
    if (config.isEnableCname() && StringUtils.hasText(config.getEndpoint()) && !config.getEndpoint().startsWith("http")) {
      return "https://" + config.getEndpoint() + "/" + objectKey;
    }
    return "https://" + config.getBucket() + "." + trimSlash(config.getEndpoint()) + "/" + objectKey;
  }

  private String extractFileName(String objectKey) {
    if (!StringUtils.hasText(objectKey)) {
      return "unnamed";
    }
    int slash = objectKey.lastIndexOf('/');
    return slash >= 0 ? objectKey.substring(slash + 1) : objectKey;
  }

  private String trimSlash(String value) {
    if (!StringUtils.hasText(value)) {
      return value;
    }
    String result = value.trim();
    while (result.endsWith("/")) {
      result = result.substring(0, result.length() - 1);
    }
    return result;
  }
}

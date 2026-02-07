package com.chenxi.astrnest.storage.s3;

import com.chenxi.astrnest.storage.StorageContext;
import com.chenxi.astrnest.storage.StorageObjectNotFoundException;
import com.chenxi.astrnest.storage.StorageProperties;
import com.chenxi.astrnest.storage.StorageStrategy;
import com.chenxi.astrnest.storage.StorageWriteException;
import com.chenxi.astrnest.storage.StoredObject;
import com.chenxi.astrnest.storage.handler.MediaMeta;
import com.chenxi.astrnest.storage.handler.StorageHandler;
import com.chenxi.astrnest.storage.handler.StorageListRequest;
import com.chenxi.astrnest.storage.handler.StorageListResult;
import com.chenxi.astrnest.storage.handler.StorageObjectSummary;
import com.chenxi.astrnest.storage.handler.StorageTokenRequest;
import com.chenxi.astrnest.storage.handler.StorageTokenResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractS3StorageHandler implements StorageHandler {

  private static final long BYTES_PER_MB = 1024L * 1024L;

  private final StorageProperties properties;
  private S3Client client;
  private S3Presigner presigner;

  protected StorageProperties properties() {
    return properties;
  }

  protected abstract StorageProperties.S3Like config();

  protected abstract StorageStrategy handlerStrategy();

  @Override
  public StorageStrategy strategy() {
    return handlerStrategy();
  }

  @Override
  public StoredObject put(MultipartFile file, StorageContext context) {
    StorageProperties.S3Like config = config();
    ensureEnabled(config);
    String objectKey = buildObjectKey(file);
    if (requiresMultipart(file.getSize(), config)) {
      multipartUpload(config, file, objectKey);
    } else {
      simpleUpload(config, file, objectKey);
    }
    return new StoredObject(
        objectKey,
        extractFileName(objectKey),
        buildPublicUrl(objectKey, config),
        file.getSize(),
        objectKey,
        strategy().name()
    );
  }

  @Override
  public void delete(String objectKey) {
    StorageProperties.S3Like config = config();
    ensureEnabled(config);
    s3().deleteObject(DeleteObjectRequest.builder().bucket(config.getBucket()).key(objectKey).build());
  }

  @Override
  public StorageListResult list(StorageListRequest request) {
    StorageProperties.S3Like config = config();
    ensureEnabled(config);
    ListObjectsV2Request.Builder builder = ListObjectsV2Request.builder()
        .bucket(config.getBucket())
        .maxKeys(Math.max(1, request.limit()));
    if (StringUtils.hasText(request.prefix())) {
      builder.prefix(request.prefix());
    }
    ListObjectsV2Response response = s3().listObjectsV2(builder.build());
    List<StorageObjectSummary> summaries = new ArrayList<>();
    response.contents().forEach(item ->
        summaries.add(new StorageObjectSummary(item.key(), item.size(), item.lastModified()))
    );
    return new StorageListResult(summaries, response.nextContinuationToken());
  }

  @Override
  public String source(String objectKey) {
    return buildPublicUrl(objectKey, config());
  }

  @Override
  public StorageTokenResponse token(StorageTokenRequest request) {
    StorageProperties.S3Like config = config();
    ensureEnabled(config);
    if (!StringUtils.hasText(request.directory())) {
      throw new IllegalArgumentException("必须指定上传目录");
    }
    String sanitizedDir = request.directory().endsWith("/") ? request.directory() : request.directory() + "/";
    String objectKey = sanitizedDir + System.currentTimeMillis();
    PresignedPutObjectRequest presigned = presigner().presignPutObject(builder -> builder
        .signatureDuration(java.time.Duration.ofMinutes(15))
        .putObjectRequest(PutObjectRequest.builder()
            .bucket(config.getBucket())
            .key(objectKey)
            .build())
    );
    return new StorageTokenResponse(
        null,
        null,
        null,
        presigned.url().toString(),
        presigned.expiration()
    );
  }

  @Override
  public MediaMeta mediaMeta(String objectKey) {
    StorageProperties.S3Like config = config();
    ensureEnabled(config);
    HeadObjectResponse head = s3().headObject(HeadObjectRequest.builder().bucket(config.getBucket()).key(objectKey).build());
    return new MediaMeta(head.contentLength(), 0, 0, head.contentType());
  }

  @Override
  public Resource load(String objectKey) {
    StorageProperties.S3Like config = config();
    ensureEnabled(config);
    ResponseInputStream<?> stream = s3().getObject(GetObjectRequest.builder().bucket(config.getBucket()).key(objectKey).build());
    if (stream == null) {
      throw new StorageObjectNotFoundException(objectKey);
    }
    return new InputStreamResource(stream);
  }

  private void simpleUpload(StorageProperties.S3Like config, MultipartFile file, String objectKey) {
    try (InputStream stream = file.getInputStream()) {
      PutObjectRequest request = PutObjectRequest.builder()
          .bucket(config.getBucket())
          .key(objectKey)
          .contentType(file.getContentType())
          .build();
      s3().putObject(request, RequestBody.fromInputStream(stream, file.getSize()));
    } catch (IOException ex) {
      throw new StorageWriteException("上传到 S3 失败", ex);
    }
  }

  private void multipartUpload(StorageProperties.S3Like config, MultipartFile file, String objectKey) {
    CreateMultipartUploadResponse init = s3().createMultipartUpload(CreateMultipartUploadRequest.builder()
        .bucket(config.getBucket())
        .key(objectKey)
        .contentType(file.getContentType())
        .build());
    String uploadId = init.uploadId();
    List<CompletedPart> parts = new ArrayList<>();
    int partNumber = 1;
    byte[] buffer = new byte[(int) resolvePartSize(config)];
    try (InputStream input = file.getInputStream()) {
      int bytesRead;
      while ((bytesRead = input.read(buffer)) != -1) {
        byte[] chunk = Arrays.copyOf(buffer, bytesRead);
        UploadPartResponse response = s3().uploadPart(UploadPartRequest.builder()
            .bucket(config.getBucket())
            .key(objectKey)
            .uploadId(uploadId)
            .partNumber(partNumber)
            .contentLength((long) bytesRead)
            .build(), RequestBody.fromBytes(chunk));
        parts.add(CompletedPart.builder()
            .partNumber(partNumber)
            .eTag(response.eTag())
            .build());
        partNumber++;
      }
      s3().completeMultipartUpload(CompleteMultipartUploadRequest.builder()
          .bucket(config.getBucket())
          .key(objectKey)
          .uploadId(uploadId)
          .multipartUpload(CompletedMultipartUpload.builder().parts(parts).build())
          .build());
    } catch (IOException | RuntimeException ex) {
      abortMultipartUpload(config, objectKey, uploadId);
      throw new StorageWriteException("S3 分片上传失败", ex);
    }
  }

  private void abortMultipartUpload(StorageProperties.S3Like config, String objectKey, String uploadId) {
    try {
      s3().abortMultipartUpload(AbortMultipartUploadRequest.builder()
          .bucket(config.getBucket())
          .key(objectKey)
          .uploadId(uploadId)
          .build());
    } catch (Exception abortEx) {
      log.warn("abort multipart upload {}:{} 失败", config.getBucket(), objectKey, abortEx);
    }
  }

  private boolean requiresMultipart(long fileSize, StorageProperties.S3Like config) {
    long threshold = config.getMultipartThresholdMb() > 0
        ? config.getMultipartThresholdMb() * BYTES_PER_MB
        : 5L * 1024 * 1024 * 1024; // 默认 5GB
    return fileSize >= threshold;
  }

  private long resolvePartSize(StorageProperties.S3Like config) {
    int partSizeMb = config.getPartSizeMb() > 0 ? config.getPartSizeMb() : 25;
    return Math.max(5, partSizeMb) * BYTES_PER_MB;
  }

  private S3Client s3() {
    if (client == null) {
      StorageProperties.S3Like cfg = config();
      ensureEnabled(cfg);
      S3ClientBuilder builder = S3Client.builder()
          .region(resolveRegion(cfg))
          .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(cfg.getAccessKey(), cfg.getSecretKey())));
      if (StringUtils.hasText(cfg.getEndpoint())) {
        builder.endpointOverride(URI.create(cfg.getEndpoint()));
      }
      S3Configuration.Builder serviceConfig = S3Configuration.builder()
          .pathStyleAccessEnabled(cfg.isPathStyle());
      if (cfg.isAccelerate()) {
        serviceConfig.accelerateModeEnabled(true);
      }
      builder.serviceConfiguration(serviceConfig.build());
      client = builder.build();
    }
    return client;
  }

  private S3Presigner presigner() {
    if (presigner == null) {
      StorageProperties.S3Like cfg = config();
      ensureEnabled(cfg);
      S3Presigner.Builder builder = S3Presigner.builder()
          .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(cfg.getAccessKey(), cfg.getSecretKey())))
          .region(resolveRegion(cfg));
      if (StringUtils.hasText(cfg.getEndpoint())) {
        builder.endpointOverride(URI.create(cfg.getEndpoint()));
      }
      presigner = builder.build();
    }
    return presigner;
  }

  private Region resolveRegion(StorageProperties.S3Like cfg) {
    return StringUtils.hasText(cfg.getRegion()) ? Region.of(cfg.getRegion()) : Region.US_EAST_1;
  }

  private void ensureEnabled(StorageProperties.S3Like config) {
    if (config == null || !config.isEnabled() || !StringUtils.hasText(config.getBucket())) {
      throw new IllegalStateException("S3 存储未启用或配置不完整");
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

  private String extractFileName(String objectKey) {
    if (!StringUtils.hasText(objectKey)) {
      return "unnamed";
    }
    int slash = objectKey.lastIndexOf('/');
    return slash >= 0 ? objectKey.substring(slash + 1) : objectKey;
  }

  private String buildPublicUrl(String objectKey, StorageProperties.S3Like config) {
    if (StringUtils.hasText(config.getCdnHost())) {
      return trimSlash(config.getCdnHost()) + "/" + objectKey;
    }
    if (StringUtils.hasText(config.getEndpoint())) {
      String endpoint = trimSlash(config.getEndpoint());
      if (config.isPathStyle()) {
        return endpoint + "/" + config.getBucket() + "/" + objectKey;
      }
      return endpoint + "/" + objectKey;
    }
    if (config.isAccelerate()) {
      return "https://" + config.getBucket() + ".s3-accelerate.amazonaws.com/" + objectKey;
    }
    String regionSegment = StringUtils.hasText(config.getRegion()) ? "-" + config.getRegion() : "";
    return "https://" + config.getBucket() + ".s3" + regionSegment + ".amazonaws.com/" + objectKey;
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

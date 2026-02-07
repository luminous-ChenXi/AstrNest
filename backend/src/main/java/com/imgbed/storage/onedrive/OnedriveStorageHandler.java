package com.imgbed.storage.onedrive;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.imgbed.storage.StorageContext;
import com.imgbed.storage.StorageObjectNotFoundException;
import com.imgbed.storage.StorageProperties;
import com.imgbed.storage.StorageStrategy;
import com.imgbed.storage.StorageWriteException;
import com.imgbed.storage.StoredObject;
import com.imgbed.storage.handler.MediaMeta;
import com.imgbed.storage.handler.StorageHandler;
import com.imgbed.storage.handler.StorageListRequest;
import com.imgbed.storage.handler.StorageListResult;
import com.imgbed.storage.handler.StorageObjectSummary;
import com.imgbed.storage.handler.StorageTokenRequest;
import com.imgbed.storage.handler.StorageTokenResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Component
@RequiredArgsConstructor
public class OnedriveStorageHandler implements StorageHandler {

  private static final long SIMPLE_UPLOAD_THRESHOLD = 4L * 1024 * 1024;
  private static final int CHUNK_SIZE = 8 * 1024 * 1024;

  private final StorageProperties properties;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final HttpClient httpClient = HttpClient.newBuilder()
      .followRedirects(Redirect.NORMAL)
      .connectTimeout(Duration.ofSeconds(30))
      .build();

  private volatile String cachedToken;
  private volatile Instant tokenExpiry = Instant.EPOCH;
  private volatile String resolvedDriveId;
  private final ConcurrentMap<String, String> shareLinkCache = new ConcurrentHashMap<>();

  @Override
  public StorageStrategy strategy() {
    return StorageStrategy.ONEDRIVE;
  }

  @Override
  public StoredObject put(MultipartFile file, StorageContext context) {
    StorageProperties.Onedrive config = config();
    ensureEnabled(config);
    String driveId = driveId();
    String objectKey = buildObjectKey(file);
    JsonNode itemNode = uploadFile(driveId, objectKey, file);
    String itemId = itemNode.path("id").asText();
    long size = itemNode.path("size").asLong(file.getSize());
    String fileName = itemNode.path("name").asText(extractFileName(objectKey));
    String webUrl = itemNode.path("webUrl").asText(objectKey);
    String publicUrl = createShareLink(driveId, itemId, objectKey);
    return new StoredObject(
        objectKey,
        fileName,
        StringUtils.hasText(publicUrl) ? publicUrl : webUrl,
        size,
        webUrl,
        strategy().name()
    );
  }

  @Override
  public void delete(String objectKey) {
    try {
      URI uri = URI.create(graphBase() + itemPath(driveId(), objectKey));
      HttpRequest request = authorizedRequest(uri).DELETE().build();
      HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
      if (response.statusCode() >= 300 && response.statusCode() != 404) {
        log.warn("删除 OneDrive 对象 {} 失败: {}", objectKey, response.statusCode());
      }
      shareLinkCache.remove(objectKey);
    } catch (IOException | InterruptedException ex) {
      handleInterrupted(ex);
      log.warn("删除 OneDrive 对象 {} 异常", objectKey, ex);
    }
  }

  @Override
  public StorageListResult list(StorageListRequest request) {
    StorageProperties.Onedrive config = config();
    ensureEnabled(config);
    String driveId = driveId();
    int limit = Math.max(1, request.limit() <= 0 ? 50 : request.limit());
    URI uri = URI.create(graphBase() + childrenPath(driveId, request.prefix()) + "?$top=" + limit);
    try {
      JsonNode node = executeJsonGet(uri);
      List<StorageObjectSummary> summaries = new ArrayList<>();
      if (node.has("value")) {
        for (JsonNode child : node.get("value")) {
          if (child.has("folder") && !child.get("folder").isNull()) {
            continue;
          }
          String name = child.path("name").asText();
          String objectKey = buildChildKey(request.prefix(), name);
          long size = child.path("size").asLong();
          Instant last = parseIsoInstant(child.path("lastModifiedDateTime").asText());
          summaries.add(new StorageObjectSummary(objectKey, size, last));
        }
      }
      String next = node.path("@odata.nextLink").asText(null);
      return new StorageListResult(summaries, next);
    } catch (IOException | InterruptedException ex) {
      handleInterrupted(ex);
      log.warn("列举 OneDrive 路径 {} 失败", request.prefix(), ex);
      return new StorageListResult(Collections.emptyList(), null);
    }
  }

  @Override
  public String source(String objectKey) {
    String cached = shareLinkCache.get(objectKey);
    if (StringUtils.hasText(cached)) {
      return cached;
    }
    JsonNode item = fetchItem(driveId(), objectKey);
    String link = createShareLink(driveId(), item.path("id").asText(), objectKey);
    return StringUtils.hasText(link) ? link : item.path("webUrl").asText();
  }

  @Override
  public StorageTokenResponse token(StorageTokenRequest request) {
    throw new UnsupportedOperationException("OneDrive 直传凭证暂未开放");
  }

  @Override
  public MediaMeta mediaMeta(String objectKey) {
    try {
      JsonNode item = fetchItem(driveId(), objectKey);
      long size = item.path("size").asLong();
      String mimeType = item.path("file").path("mimeType").asText(null);
      int width = item.path("image").path("width").asInt(0);
      int height = item.path("image").path("height").asInt(0);
      if (width == 0) {
        width = item.path("photo").path("width").asInt(0);
      }
      if (height == 0) {
        height = item.path("photo").path("height").asInt(0);
      }
      return new MediaMeta(size, width, height, mimeType);
    } catch (StorageObjectNotFoundException ex) {
      throw ex;
    } catch (Exception ex) {
      log.warn("获取 OneDrive 元信息失败: {}", objectKey, ex);
      return new MediaMeta(0, 0, 0, null);
    }
  }

  @Override
  public Resource load(String objectKey) {
    try {
      URI uri = URI.create(graphBase() + itemPath(driveId(), objectKey) + ":/content");
      HttpRequest request = authorizedRequest(uri).GET().build();
      HttpResponse<byte[]> response = httpClient.send(request, BodyHandlers.ofByteArray());
      if (response.statusCode() >= 300) {
        throw new StorageObjectNotFoundException(objectKey);
      }
      return new ByteArrayResource(response.body());
    } catch (IOException | InterruptedException ex) {
      handleInterrupted(ex);
      throw new StorageObjectNotFoundException(objectKey);
    }
  }

  private StorageProperties.Onedrive config() {
    return properties.getOnedrive();
  }

  private void ensureEnabled(StorageProperties.Onedrive config) {
    if (config == null || !config.isEnabled()) {
      throw new IllegalStateException("OneDrive 存储未启用");
    }
    if (!StringUtils.hasText(config.getClientId()) || !StringUtils.hasText(config.getClientSecret())) {
      throw new IllegalStateException("OneDrive clientId/clientSecret 未配置");
    }
  }

  private JsonNode uploadFile(String driveId, String objectKey, MultipartFile file) {
    try {
      if (file.getSize() <= SIMPLE_UPLOAD_THRESHOLD) {
        return simpleUpload(driveId, objectKey, file);
      }
      return chunkedUpload(driveId, objectKey, file);
    } catch (IOException | InterruptedException ex) {
      handleInterrupted(ex);
      throw new StorageWriteException("上传 OneDrive 文件失败", ex);
    }
  }

  private JsonNode simpleUpload(String driveId, String objectKey, MultipartFile file)
      throws IOException, InterruptedException {
    URI uri = URI.create(graphBase() + itemPath(driveId, objectKey) + ":/content");
    BodyPublisher publisher = HttpRequest.BodyPublishers.ofInputStream(() -> {
      try {
        InputStream stream = file.getInputStream();
        return stream;
      } catch (IOException ex) {
        throw new UncheckedIOException(ex);
      }
    });
    HttpRequest request = authorizedRequest(uri)
        .header("Content-Type", contentType(file))
        .PUT(publisher)
        .build();
    HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
    if (response.statusCode() >= 300) {
      throw new StorageWriteException("OneDrive 简单上传失败: " + response.body());
    }
    return objectMapper.readTree(response.body());
  }

  @SuppressWarnings("null")
  private JsonNode chunkedUpload(String driveId, String objectKey, MultipartFile file)
      throws IOException, InterruptedException {
    URI sessionUri = URI.create(graphBase() + itemPath(driveId, objectKey) + ":/createUploadSession");
    String payload = "{\"item\":{\"@microsoft.graph.conflictBehavior\":\"replace\"}}";
    HttpRequest sessionRequest = authorizedRequest(sessionUri)
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(payload))
        .build();
    HttpResponse<String> sessionResponse = httpClient.send(sessionRequest, BodyHandlers.ofString());
    if (sessionResponse.statusCode() >= 300) {
      throw new StorageWriteException("创建 OneDrive 上传会话失败: " + sessionResponse.body());
    }
    String uploadUrl = objectMapper.readTree(sessionResponse.body()).path("uploadUrl").asText();
    if (!StringUtils.hasText(uploadUrl)) {
      throw new StorageWriteException("OneDrive 上传会话未返回 uploadUrl");
    }
    long total = file.getSize();
    long uploaded = 0L;
    byte[] buffer = new byte[CHUNK_SIZE];
    try (InputStream input = file.getInputStream()) {
      int bytesRead;
      while ((bytesRead = input.read(buffer)) != -1) {
        long start = uploaded;
        long end = uploaded + bytesRead - 1;
        byte[] chunk = Arrays.copyOf(buffer, bytesRead);
        HttpRequest chunkRequest = HttpRequest.newBuilder(URI.create(uploadUrl))
            .header("Authorization", "Bearer " + accessToken())
            .header("Content-Length", String.valueOf(bytesRead))
            .header("Content-Range", "bytes " + start + "-" + end + "/" + total)
            .PUT(HttpRequest.BodyPublishers.ofByteArray(Objects.requireNonNull(chunk)))
            .build();
        HttpResponse<String> chunkResponse = httpClient.send(chunkRequest, BodyHandlers.ofString());
        if (chunkResponse.statusCode() == 200 || chunkResponse.statusCode() == 201) {
          return objectMapper.readTree(chunkResponse.body());
        }
        if (chunkResponse.statusCode() != 202) {
          throw new StorageWriteException("OneDrive 分片上传失败: " + chunkResponse.body());
        }
        uploaded += bytesRead;
      }
    }
    throw new StorageWriteException("OneDrive 分片上传未完成");
  }

  private String createShareLink(String driveId, String itemId, String cacheKey) {
    String cached = shareLinkCache.get(cacheKey);
    if (StringUtils.hasText(cached)) {
      return cached;
    }
    try {
      URI uri = URI.create(graphBase() + "/drives/" + driveId + "/items/" + itemId + "/createLink");
      HttpRequest request = authorizedRequest(uri)
          .header("Content-Type", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString("{\"type\":\"view\",\"scope\":\"anonymous\"}"))
          .build();
      HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
      if (response.statusCode() >= 300) {
        log.warn("创建 OneDrive 分享链接失败: {}", response.body());
        return null;
      }
      String link = objectMapper.readTree(response.body()).path("link").path("webUrl").asText(null);
      if (StringUtils.hasText(link)) {
        shareLinkCache.put(cacheKey, link);
      }
      return link;
    } catch (IOException | InterruptedException ex) {
      handleInterrupted(ex);
      log.warn("创建 OneDrive 分享链接异常", ex);
      return null;
    }
  }

  private JsonNode fetchItem(String driveId, String objectKey) {
    URI uri = URI.create(graphBase() + itemPath(driveId, objectKey));
    try {
      JsonNode node = executeJsonGet(uri);
      if (node.isMissingNode() || node.isEmpty()) {
        throw new StorageObjectNotFoundException(objectKey);
      }
      return node;
    } catch (IOException | InterruptedException ex) {
      handleInterrupted(ex);
      throw new StorageObjectNotFoundException(objectKey);
    }
  }

  private JsonNode executeJsonGet(URI uri) throws IOException, InterruptedException {
    HttpRequest request = authorizedRequest(uri).GET().build();
    HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
    if (response.statusCode() >= 300) {
      throw new StorageWriteException("OneDrive 请求失败: " + response.body());
    }
    return objectMapper.readTree(response.body());
  }

  private HttpRequest.Builder authorizedRequest(URI uri) {
    return HttpRequest.newBuilder(uri)
        .header("Authorization", "Bearer " + accessToken());
  }

  private String accessToken() {
    Instant now = Instant.now();
    if (StringUtils.hasText(cachedToken) && now.isBefore(tokenExpiry.minusSeconds(60))) {
      return cachedToken;
    }
    synchronized (this) {
      if (StringUtils.hasText(cachedToken) && now.isBefore(tokenExpiry.minusSeconds(60))) {
        return cachedToken;
      }
      StorageProperties.Onedrive config = config();
      Map<String, String> form = new HashMap<>();
      String authority = resolveAuthority(config);
      URI tokenUri = URI.create("https://login.microsoftonline.com/" + authority + "/oauth2/v2.0/token");
      if (StringUtils.hasText(config.getRefreshToken())) {
        form.put("grant_type", "refresh_token");
        form.put("refresh_token", config.getRefreshToken());
        form.put("scope", "https://graph.microsoft.com/.default offline_access Files.ReadWrite.All");
        form.put("redirect_uri", config.getRedirectUri());
      } else {
        form.put("grant_type", "client_credentials");
        form.put("scope", "https://graph.microsoft.com/.default");
      }
      form.put("client_id", config.getClientId());
      form.put("client_secret", config.getClientSecret());
      HttpRequest request = HttpRequest.newBuilder(tokenUri)
          .header("Content-Type", "application/x-www-form-urlencoded")
          .POST(HttpRequest.BodyPublishers.ofString(encodeForm(form)))
          .build();
      try {
        HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
        if (response.statusCode() >= 300) {
          throw new StorageWriteException("获取 OneDrive Token 失败: " + response.body());
        }
        JsonNode node = objectMapper.readTree(response.body());
        cachedToken = node.path("access_token").asText();
        int expiresIn = node.path("expires_in").asInt(3600);
        tokenExpiry = Instant.now().plusSeconds(Math.max(120, expiresIn - 60));
        return cachedToken;
      } catch (IOException | InterruptedException ex) {
        handleInterrupted(ex);
        throw new StorageWriteException("获取 OneDrive Token 出错", ex);
      }
    }
  }

  private String driveId() {
    if (StringUtils.hasText(resolvedDriveId)) {
      return resolvedDriveId;
    }
    synchronized (this) {
      if (StringUtils.hasText(resolvedDriveId)) {
        return resolvedDriveId;
      }
      StorageProperties.Onedrive config = config();
      if (StringUtils.hasText(config.getDriveId())) {
        resolvedDriveId = config.getDriveId();
        return resolvedDriveId;
      }
      if (!StringUtils.hasText(config.getSiteId())) {
        throw new IllegalStateException("OneDrive 未配置 driveId 或 siteId");
      }
      try {
        URI uri = URI.create(graphBase() + "/sites/" + config.getSiteId() + "/drive");
        JsonNode node = executeJsonGet(uri);
        resolvedDriveId = node.path("id").asText();
        if (!StringUtils.hasText(resolvedDriveId)) {
          throw new IllegalStateException("无法从 siteId 解析 OneDrive driveId");
        }
        return resolvedDriveId;
      } catch (IOException | InterruptedException ex) {
        handleInterrupted(ex);
        throw new StorageWriteException("解析 OneDrive driveId 失败", ex);
      }
    }
  }

  private String graphBase() {
    String base = config().getBaseUrl();
    if (!StringUtils.hasText(base)) {
      base = "https://graph.microsoft.com/v1.0";
    }
    if (base.endsWith("/")) {
      return base.substring(0, base.length() - 1);
    }
    return base;
  }

  private String resolveAuthority(StorageProperties.Onedrive config) {
    if (StringUtils.hasText(config.getTenantId())) {
      return config.getTenantId();
    }
    if ("business".equalsIgnoreCase(config.getDriveType())) {
      return "organizations";
    }
    return "consumers";
  }

  private String encodeForm(Map<String, String> form) {
    StringBuilder builder = new StringBuilder();
    for (Map.Entry<String, String> entry : form.entrySet()) {
      if (!StringUtils.hasText(entry.getValue())) {
        continue;
      }
      if (builder.length() > 0) {
        builder.append('&');
      }
      builder.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8))
          .append('=')
          .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
    }
    return builder.toString();
  }

  private String contentType(MultipartFile file) {
    return StringUtils.hasText(file.getContentType()) ? file.getContentType() : "application/octet-stream";
  }

  private String buildObjectKey(MultipartFile file) {
    ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
    String year = String.format("%04d", now.getYear());
    String month = String.format("%02d", now.getMonthValue());
    return year + "/" + month + "/" + resolveFileName(file);
  }

  private String resolveFileName(MultipartFile file) {
    String original = sanitizeFileName(file.getOriginalFilename());
    if (!StringUtils.hasText(original) || "blob".equalsIgnoreCase(original)) {
      return "luminouscx" + System.currentTimeMillis() + extensionFromContentType(file.getContentType());
    }
    return original;
  }

  private String sanitizeFileName(String value) {
    if (!StringUtils.hasText(value)) {
      return "";
    }
    String sanitized = value.replace("\\", "_").replace("/", "_").trim();
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

  private String itemPath(String driveId, String objectKey) {
    String path = encodePath(objectKey);
    if (!StringUtils.hasText(path)) {
      return "/drives/" + driveId + "/root";
    }
    return "/drives/" + driveId + "/root:/" + path;
  }

  private String childrenPath(String driveId, String prefix) {
    if (!StringUtils.hasText(prefix)) {
      return "/drives/" + driveId + "/root/children";
    }
    return "/drives/" + driveId + "/root:/" + encodePath(prefix) + ":/children";
  }

  private String encodePath(String path) {
    if (!StringUtils.hasText(path)) {
      return "";
    }
    String[] segments = path.split("/");
    List<String> encoded = new ArrayList<>();
    for (String segment : segments) {
      if (!StringUtils.hasText(segment)) {
        continue;
      }
      encoded.add(URLEncoder.encode(segment, StandardCharsets.UTF_8).replace("+", "%20"));
    }
    return String.join("/", encoded);
  }

  private String buildChildKey(String prefix, String name) {
    if (!StringUtils.hasText(prefix)) {
      return name;
    }
    return prefix.endsWith("/") ? prefix + name : prefix + "/" + name;
  }

  private Instant parseIsoInstant(String value) {
    if (!StringUtils.hasText(value)) {
      return Instant.EPOCH;
    }
    try {
      return Instant.parse(value);
    } catch (Exception ex) {
      return Instant.EPOCH;
    }
  }

  private void handleInterrupted(Exception ex) {
    if (ex instanceof InterruptedException) {
      Thread.currentThread().interrupt();
    }
  }
}

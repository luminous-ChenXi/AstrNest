package com.chenxi.astrnest.storage;

import com.chenxi.astrnest.system.SystemConfigService;
import com.chenxi.astrnest.upload.record.UploadRecord;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class PublicAssetUrlResolver {

  private static final String DEFAULT_PUBLIC_BASE = "/upload";
  private static final String VIOLATION_PLACEHOLDER_PATH = "assets/img/2048x2048_违规.png";

  private final StorageProperties storageProperties;
  private final SystemConfigService systemConfigService;

  public String resolve(UploadRecord record) {
    if (record == null) {
      return null;
    }
    if (record.isViolation() && StringUtils.hasText(record.getPublicUrl())) {
      return record.getPublicUrl();
    }
    if (isLocalProvider(record.getStorageProvider()) && StringUtils.hasText(record.getObjectKey())) {
      return buildLocalPublicUrl(record.getObjectKey());
    }
    String override = buildAssetDomainOverride(record.getObjectKey());
    if (override != null) {
      return override;
    }
    return record.getPublicUrl();
  }

  public String resolveStoredObject(StoredObject storedObject) {
    if (storedObject == null) {
      return null;
    }
    if (isLocalProvider(storedObject.providerKey()) && StringUtils.hasText(storedObject.objectKey())) {
      return buildLocalPublicUrl(storedObject.objectKey());
    }
    String override = buildAssetDomainOverride(storedObject.objectKey());
    if (override != null) {
      return override;
    }
    return storedObject.publicUrl();
  }

  public String buildLocalPublicUrl(String objectKey) {
    String sanitizedKey = sanitizeObjectKey(objectKey);
    String base = storageProperties.getLocal().getPublicBaseUrl();
    if (!StringUtils.hasText(base)) {
      base = DEFAULT_PUBLIC_BASE;
    }
    base = base.trim();
    if (isAbsoluteUrl(base)) {
      return appendPath(trimTrailingSlash(base), sanitizedKey);
    }
    String normalizedPath = normalizePathPrefix(base);
    String domain = systemConfigService.currentAssetDomain();
    if (StringUtils.hasText(domain)) {
      return appendPath(trimTrailingSlash(domain) + normalizedPath, sanitizedKey);
    }
    return appendPath(normalizedPath, sanitizedKey);
  }

  public String violationPlaceholderUrl() {
    return resolveAssetPath(VIOLATION_PLACEHOLDER_PATH);
  }

  public String resolveAssetPath(String relativePath) {
    if (!StringUtils.hasText(relativePath)) {
      return null;
    }
    String sanitized = relativePath.trim();
    while (sanitized.startsWith("/")) {
      sanitized = sanitized.substring(1);
    }
    String path = "/" + sanitized;
    String domain = systemConfigService.currentAssetDomain();
    if (StringUtils.hasText(domain)) {
      return trimTrailingSlash(domain) + path;
    }
    return path;
  }

  private String buildAssetDomainOverride(String objectKey) {
    String domain = systemConfigService.currentAssetDomain();
    if (!StringUtils.hasText(domain) || !StringUtils.hasText(objectKey)) {
      return null;
    }
    return appendPath(trimTrailingSlash(domain), sanitizeObjectKey(objectKey));
  }

  private String sanitizeObjectKey(String objectKey) {
    if (!StringUtils.hasText(objectKey)) {
      return "";
    }
    String sanitized = objectKey.trim();
    while (sanitized.startsWith("/")) {
      sanitized = sanitized.substring(1);
    }
    return sanitized;
  }

  private boolean isLocalProvider(String providerKey) {
    if (!StringUtils.hasText(providerKey)) {
      return true;
    }
    String normalized = providerKey.trim().toUpperCase(Locale.ROOT);
    return "LOCAL".equals(normalized) || "LOCAL_DISK".equals(normalized);
  }

  private boolean isAbsoluteUrl(String value) {
    String normalized = value.toLowerCase(Locale.ROOT);
    return normalized.startsWith("http://") || normalized.startsWith("https://");
  }

  private String normalizePathPrefix(String value) {
    String normalized = value.replace('\\', '/').trim();
    if (!normalized.startsWith("/")) {
      normalized = "/" + normalized;
    }
    while (normalized.endsWith("/") && normalized.length() > 1) {
      normalized = normalized.substring(0, normalized.length() - 1);
    }
    return normalized;
  }

  private String trimTrailingSlash(String value) {
    String result = value.trim();
    while (result.endsWith("/") && !result.endsWith("://")) {
      result = result.substring(0, result.length() - 1);
    }
    return result;
  }

  private String appendPath(String prefix, String key) {
    if (!StringUtils.hasText(key)) {
      return prefix;
    }
    return prefix + "/" + key;
  }
}

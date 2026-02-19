package com.chenxi.astrnest.storage;

import java.util.Map;
import org.springframework.lang.Nullable;

public record StorageContext(
    @Nullable StorageStrategy strategy,
    @Nullable String providerKey,
    @Nullable String visibility,
    Map<String, String> metadata
) {

  public static final String METADATA_MEDIA_CATEGORY = "mediaCategory";

  public static StorageContext localPublicContext() {
    return new StorageContext(null, null, "PUBLIC", Map.of());
  }

  public static StorageContext localPublicContext(Map<String, String> metadata) {
    return new StorageContext(null, null, "PUBLIC", metadata == null ? Map.of() : metadata);
  }

  public Map<String, String> safeMetadata() {
    return metadata == null ? Map.of() : metadata;
  }
}

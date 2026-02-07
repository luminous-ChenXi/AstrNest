package com.imgbed.storage;

import java.util.Map;
import org.springframework.lang.Nullable;

public record StorageContext(
    @Nullable StorageStrategy strategy,
    @Nullable String providerKey,
    @Nullable String visibility,
    Map<String, String> metadata
) {
  public static StorageContext localPublicContext() {
    return new StorageContext(null, null, "PUBLIC", Map.of());
  }
}

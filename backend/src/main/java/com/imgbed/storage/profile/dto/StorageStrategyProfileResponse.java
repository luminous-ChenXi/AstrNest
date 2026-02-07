package com.imgbed.storage.profile.dto;

import com.imgbed.storage.StorageStrategy;
import java.time.Instant;
import java.util.Map;

public record StorageStrategyProfileResponse(
    Long id,
    StorageStrategy strategy,
    String name,
    String displayName,
    String description,
    boolean active,
    boolean enabled,
    Map<String, Object> config,
    String updatedBy,
    Instant createdAt,
    Instant updatedAt
) {}

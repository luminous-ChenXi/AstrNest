package com.imgbed.security.apikey.dto;

import java.time.Instant;

public record ApiKeyResponse(
    Long id,
    String name,
    String description,
    String maskedKey,
    boolean active,
    long requestCount,
    int requestsToday,
    int dailyQuota,
    Instant createdAt,
    Instant lastUsedAt
) {}

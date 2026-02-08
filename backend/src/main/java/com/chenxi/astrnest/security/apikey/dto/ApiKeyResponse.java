package com.chenxi.astrnest.security.apikey.dto;

import java.time.Instant;

public record ApiKeyResponse(
    Long id,
    String name,
    String description,
    String maskedKey,
    boolean active,
    long requestCount,
    int requestsToday,
    int requestsCurrentMinute,
    int dailyQuota,
    int perMinuteQuota,
    long uploadCount,
    long todayUploadCount,
    Instant lastUploadAt,
    Instant createdAt,
    Instant lastUsedAt,
    ApiKeyOwnerInfo owner
) {}

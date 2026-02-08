package com.chenxi.astrnest.security.apikey.dto;

import java.time.Instant;

public record ApiKeyUsageAggregate(
    Long apiKeyId,
    long uploadCount,
    long todayUploadCount,
    Instant lastUploadAt
) {}

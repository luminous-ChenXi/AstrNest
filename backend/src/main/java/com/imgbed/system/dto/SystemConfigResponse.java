package com.imgbed.system.dto;

import java.time.Instant;

public record SystemConfigResponse(
    long maxUploadBytes,
    double maxUploadMegabytes,
    int dailyUploadCountLimit,
    long userStorageQuotaBytes,
    double userStorageQuotaGigabytes,
    boolean registrationEnabled,
    boolean guestLikeEnabled,
    String assetDomain,
    String customFooterHtml,
    Instant updatedAt,
    String updatedBy
) {
}

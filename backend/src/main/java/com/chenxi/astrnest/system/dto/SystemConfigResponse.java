package com.chenxi.astrnest.system.dto;

import java.time.Instant;

public record SystemConfigResponse(
    long maxUploadBytes,
    double maxUploadMegabytes,
    long maxVideoUploadBytes,
    double maxVideoUploadMegabytes,
    int dailyUploadCountLimit,
    long userStorageQuotaBytes,
    double userStorageQuotaGigabytes,
    boolean registrationEnabled,
    boolean guestLikeEnabled,
    int autoCleanupDays,
    boolean videoChunkUploadEnabled,
    int videoChunkSizeMb,
    String assetDomain,
    String customFooterHtml,
    boolean aiModerationEnabled,
    boolean aiLabelingEnabled,
    String aiTencentSecretId,
    String aiTencentSecretKey,
    String aiTencentRegion,
    String aiTencentBucket,
    String aiTencentDetectScenes,
    int aiModerationBlockConfidence,
    int aiModerationReviewConfidence,
    int aiLabelMinConfidence,
    Instant updatedAt,
    String updatedBy
) {
}

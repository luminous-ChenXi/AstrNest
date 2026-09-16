package com.chenxi.astrnest.user.dto;

import java.util.List;

public record UserOverviewResponse(
    long totalUploads,
    long todayUploads,
    long storageBytes,
    Integer totalUploadLimit,
    int totalRemaining,
    Long storageQuotaMb,
    long storageRemainingBytes,
    List<UserUploadItemResponse> latestUploads
) {}

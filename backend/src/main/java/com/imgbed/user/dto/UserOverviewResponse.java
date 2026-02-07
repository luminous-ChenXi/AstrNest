package com.imgbed.user.dto;

import java.util.List;

public record UserOverviewResponse(
    long totalUploads,
    long todayUploads,
    long storageBytes,
    Integer dailyUploadLimit,
    int dailyRemaining,
    Long storageQuotaMb,
    long storageRemainingBytes,
    List<UserUploadItemResponse> latestUploads
) {}

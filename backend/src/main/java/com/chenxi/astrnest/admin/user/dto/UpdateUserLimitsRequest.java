package com.chenxi.astrnest.admin.user.dto;

import jakarta.validation.constraints.Min;

public record UpdateUserLimitsRequest(
    @Min(0) Integer dailyUploadLimit,
    @Min(0) Long storageQuotaMb
) {}

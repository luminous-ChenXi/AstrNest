package com.chenxi.astrnest.security.apikey.dto;

public record ApiKeyDashboardResponse(
    long totalKeys,
    long activeKeys,
    long totalRequests,
    long todaysRequests,
    long totalOwners,
    long totalUploadsViaApi,
    long todayUploadsViaApi
) {}

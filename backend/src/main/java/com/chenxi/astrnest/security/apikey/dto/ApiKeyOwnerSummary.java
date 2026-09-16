package com.chenxi.astrnest.security.apikey.dto;

public record ApiKeyOwnerSummary(
    Long ownerId,
    String username,
    String displayName,
    long keyCount,
    long activeKeyCount,
    long totalRequests,
    long todaysRequests,
    long uploadCount,
    long storageBytes
) {}

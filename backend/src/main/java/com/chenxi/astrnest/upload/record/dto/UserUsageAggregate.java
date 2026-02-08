package com.chenxi.astrnest.upload.record.dto;

public record UserUsageAggregate(
    Long userId,
    long uploadCount,
    long storageBytes,
    long likeCount
) {}

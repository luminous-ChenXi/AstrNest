package com.imgbed.upload.record.dto;

public record UserUsageAggregate(
    Long userId,
    long uploadCount,
    long storageBytes,
    long likeCount
) {}

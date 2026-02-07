package com.imgbed.user.dto;

import java.time.Instant;

public record UserUploadItemResponse(
    Long id,
    String fileName,
    String publicUrl,
    long size,
    String reviewStatus,
    Instant uploadedAt,
    boolean publicAccessible,
    long likeCount,
    long invokeCount,
    String storagePath,
    String storageProvider
) {}

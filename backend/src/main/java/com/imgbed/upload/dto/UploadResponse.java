package com.imgbed.upload.dto;

import java.time.Instant;

public record UploadResponse(
    String fileName,
    String originalFileName,
    String objectKey,
    String publicUrl,
    long size,
    Instant uploadedAt,
    String reviewStatus,
    boolean publicAccessible,
    long likeCount,
    long invokeCount
) {}

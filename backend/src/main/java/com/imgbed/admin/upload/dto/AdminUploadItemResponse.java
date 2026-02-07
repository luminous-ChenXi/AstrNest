package com.imgbed.admin.upload.dto;

import java.time.Instant;

public record AdminUploadItemResponse(
    Long id,
    String fileName,
    String objectKey,
    String publicUrl,
    long size,
    String contentType,
    String reviewStatus,
    boolean violation,
    boolean publicAccessible,
    long likeCount,
    long invokeCount,
    String uploaderUsername,
    String uploaderDisplayName,
    String uploaderEmail,
    String uploaderIp,
    String storageProvider,
    String storageMode,
    Instant uploadedAt
) {
}

package com.chenxi.astrnest.admin.upload.dto;

import com.chenxi.astrnest.tag.dto.ChenxiTagResponse;
import com.chenxi.astrnest.upload.media.MediaCategory;
import java.time.Instant;
import java.util.List;

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
    Instant uploadedAt,
    List<ChenxiTagResponse> tags,
    MediaCategory mediaCategory
) {
}

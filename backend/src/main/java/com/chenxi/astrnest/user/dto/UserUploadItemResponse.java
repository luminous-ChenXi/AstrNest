package com.chenxi.astrnest.user.dto;

import com.chenxi.astrnest.tag.dto.ChenxiTagResponse;
import com.chenxi.astrnest.upload.dto.AiReviewFeedback;
import java.time.Instant;
import java.util.List;

public record UserUploadItemResponse(
    Long id,
    String fileName,
    String mediaUuid,
    String mediaCategory,
    String publicUrl,
    String thumbnailUrl,
    String embedUrl,
    long size,
    String reviewStatus,
    Instant uploadedAt,
    boolean publicAccessible,
    long likeCount,
    long invokeCount,
    String storagePath,
    String storageProvider,
    List<ChenxiTagResponse> tags,
    AiReviewFeedback aiReview
) {}

package com.chenxi.astrnest.upload.dto;

import com.chenxi.astrnest.tag.dto.ChenxiTagResponse;
import java.time.Instant;
import java.util.List;

public record UploadResponse(
    String fileName,
    String originalFileName,
    String objectKey,
    String mediaUuid,
    String mediaCategory,
    String publicUrl,
    String thumbnailUrl,
    String embedUrl,
    long size,
    Instant uploadedAt,
    String reviewStatus,
    boolean publicAccessible,
    long likeCount,
    long invokeCount,
    List<ChenxiTagResponse> tags,
    AiReviewFeedback aiReview
) {}

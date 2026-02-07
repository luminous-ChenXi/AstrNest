package com.imgbed.gallery.dto;

import java.time.Instant;

public record PublicGalleryItemResponse(
    Long id,
    String fileName,
    String publicUrl,
    String objectKey,
    long size,
    Instant uploadedAt,
    Long ownerId,
    String ownerDisplayName,
    String ownerAvatarUrl,
    long likeCount,
    long invokeCount,
    boolean likedByMe,
    PublicRecentLikeResponse latestLike
) {}

package com.chenxi.astrnest.gallery.dto;

import java.time.Instant;

public record PublicRecentLikeResponse(
    String displayName,
    Long userId,
    String avatarUrl,
    boolean guest,
    Instant likedAt
) {
}

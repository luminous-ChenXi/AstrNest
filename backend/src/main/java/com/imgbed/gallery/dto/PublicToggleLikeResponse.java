package com.imgbed.gallery.dto;

public record PublicToggleLikeResponse(
    long likeCount,
    boolean liked,
    PublicRecentLikeResponse latestLike
) {
}

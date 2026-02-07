package com.chenxi.astrnest.gallery.dto;

public record PublicToggleLikeResponse(
    long likeCount,
    boolean liked,
    PublicRecentLikeResponse latestLike
) {
}

package com.chenxi.astrnest.user.dto;

public record PublicUserProfileResponse(
    Long id,
    String displayName,
    String email,
    String avatarUrl,
    String signature,
    long uploadCount,
    long storageBytes,
    long likeCount
) {}

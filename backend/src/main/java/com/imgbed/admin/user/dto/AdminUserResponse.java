package com.imgbed.admin.user.dto;

import java.time.Instant;
import java.util.Set;

public record AdminUserResponse(
    Long id,
    String username,
    String displayName,
    String email,
    String avatarUrl,
    String signature,
    Set<String> roles,
    boolean active,
    Instant createdAt,
    long uploadCount,
    long storageBytes,
    long likeCount,
    Integer dailyUploadLimit,
    Long storageQuotaMb
) {}

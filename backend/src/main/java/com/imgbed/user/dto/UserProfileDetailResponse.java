package com.imgbed.user.dto;

import java.time.Instant;
import java.util.Set;

public record UserProfileDetailResponse(
    Long id,
    String username,
    String displayName,
    String email,
    String avatarUrl,
    String website,
    String signature,
    String location,
    boolean active,
    Instant joinedAt,
    String loginIpHistory,
    String lastLoginIp,
    Instant lastLoginAt,
    Set<String> roles
) {}

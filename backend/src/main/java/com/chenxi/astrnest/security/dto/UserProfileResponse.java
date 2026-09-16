package com.chenxi.astrnest.security.dto;

import java.time.Instant;
import java.util.Set;

public record UserProfileResponse(
    Long id,
    String username,
    String displayName,
    String email,
    String avatarUrl,
    String website,
    String signature,
    String location,
    String loginIpHistory,
    String lastLoginIp,
    Instant lastLoginAt,
    Set<String> roles
) {}

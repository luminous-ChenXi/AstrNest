package com.chenxi.astrnest.user.dto;

import com.chenxi.astrnest.security.dto.UserProfileResponse;

public record LoginResponse(
    String token,
    UserProfileResponse profile
) {}

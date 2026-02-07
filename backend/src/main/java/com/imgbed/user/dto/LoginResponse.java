package com.imgbed.user.dto;

import com.imgbed.security.dto.UserProfileResponse;

public record LoginResponse(
    String token,
    UserProfileResponse profile
) {}

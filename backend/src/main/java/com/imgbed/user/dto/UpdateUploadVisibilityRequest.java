package com.imgbed.user.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateUploadVisibilityRequest(@NotNull Boolean publicAccessible) {}

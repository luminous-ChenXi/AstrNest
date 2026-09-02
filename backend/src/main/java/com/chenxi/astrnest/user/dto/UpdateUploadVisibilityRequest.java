package com.chenxi.astrnest.user.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateUploadVisibilityRequest(@NotNull Boolean publicAccessible) {}

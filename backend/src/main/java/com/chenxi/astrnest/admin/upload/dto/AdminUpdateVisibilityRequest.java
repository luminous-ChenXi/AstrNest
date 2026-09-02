package com.chenxi.astrnest.admin.upload.dto;

import jakarta.validation.constraints.NotNull;

public record AdminUpdateVisibilityRequest(@NotNull Boolean publicAccessible) {
}

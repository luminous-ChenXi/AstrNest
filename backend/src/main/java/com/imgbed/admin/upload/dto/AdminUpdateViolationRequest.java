package com.imgbed.admin.upload.dto;

import jakarta.validation.constraints.NotNull;

public record AdminUpdateViolationRequest(@NotNull Boolean violation) {
}

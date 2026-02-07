package com.imgbed.security.apikey.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateApiKeyRequest(
    @NotBlank @Size(max = 120) String name,
    @Size(max = 255) String description,
    @Min(100) @Max(100000) Integer dailyQuota
) {}

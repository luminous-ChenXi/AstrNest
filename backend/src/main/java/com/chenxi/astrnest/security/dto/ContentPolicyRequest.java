package com.chenxi.astrnest.security.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record ContentPolicyRequest(
    boolean nsfwDetectionEnabled,
    boolean violenceDetectionEnabled,
    @Min(0) @Max(10) int manualReviewThreshold,
    String webhookUrl
) {}

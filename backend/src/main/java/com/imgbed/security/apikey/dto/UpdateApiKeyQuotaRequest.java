package com.imgbed.security.apikey.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record UpdateApiKeyQuotaRequest(@Min(100) @Max(100000) int dailyQuota) {}

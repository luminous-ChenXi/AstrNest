package com.chenxi.astrnest.security.apikey.dto;

public record CreateApiKeyResponse(
    ApiKeyResponse key,
    String plainValue
) {}

package com.imgbed.security.apikey.dto;

public record CreateApiKeyResponse(
    ApiKeyResponse key,
    String plainValue
) {}

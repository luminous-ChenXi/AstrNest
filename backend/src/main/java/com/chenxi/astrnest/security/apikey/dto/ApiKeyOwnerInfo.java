package com.chenxi.astrnest.security.apikey.dto;

public record ApiKeyOwnerInfo(
    Long id,
    String username,
    String displayName
) {}

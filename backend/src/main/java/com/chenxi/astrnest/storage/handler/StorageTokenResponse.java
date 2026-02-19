package com.chenxi.astrnest.storage.handler;

import java.time.Instant;

public record StorageTokenResponse(
    String credential,
    String policy,
    String signature,
    String uploadUrl,
    Instant expiresAt
) {}

package com.chenxi.astrnest.storage.handler;

public record StorageTokenRequest(
    String directory,
    String callbackUrl
) {}

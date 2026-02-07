package com.imgbed.storage.handler;

public record StorageTokenRequest(
    String directory,
    String callbackUrl
) {}

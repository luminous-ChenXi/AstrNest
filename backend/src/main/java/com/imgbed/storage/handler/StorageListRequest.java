package com.imgbed.storage.handler;

public record StorageListRequest(
    String prefix,
    int limit
) {}

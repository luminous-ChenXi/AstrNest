package com.chenxi.astrnest.storage.handler;

public record StorageListRequest(
    String prefix,
    int limit
) {}

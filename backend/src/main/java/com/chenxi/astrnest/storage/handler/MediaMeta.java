package com.chenxi.astrnest.storage.handler;

public record MediaMeta(
    long size,
    int width,
    int height,
    String contentType
) {}

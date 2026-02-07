package com.imgbed.storage.handler;

public record MediaMeta(
    long size,
    int width,
    int height,
    String contentType
) {}

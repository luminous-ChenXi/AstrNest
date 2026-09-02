package com.chenxi.astrnest.storage.handler;

import java.time.Instant;

public record StorageObjectSummary(
    String objectKey,
    long size,
    Instant lastModified
) {}

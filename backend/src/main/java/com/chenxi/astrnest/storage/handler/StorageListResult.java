package com.chenxi.astrnest.storage.handler;

import java.util.List;

public record StorageListResult(
    List<StorageObjectSummary> objects,
    String nextMarker
) {}

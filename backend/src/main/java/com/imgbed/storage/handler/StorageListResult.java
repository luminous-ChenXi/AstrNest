package com.imgbed.storage.handler;

import java.util.List;

public record StorageListResult(
    List<StorageObjectSummary> objects,
    String nextMarker
) {}

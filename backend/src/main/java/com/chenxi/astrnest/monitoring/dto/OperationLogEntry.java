package com.chenxi.astrnest.monitoring.dto;

import java.time.Instant;

public record OperationLogEntry(String id, String action, String user, Instant timestamp) {}

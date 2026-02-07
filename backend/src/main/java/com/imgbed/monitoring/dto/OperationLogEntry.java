package com.imgbed.monitoring.dto;

import java.time.Instant;

public record OperationLogEntry(String id, String action, String user, Instant timestamp) {}

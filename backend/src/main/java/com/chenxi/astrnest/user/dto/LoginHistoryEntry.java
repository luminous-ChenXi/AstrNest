package com.chenxi.astrnest.user.dto;

import java.time.Instant;

public record LoginHistoryEntry(
    Long id,
    Instant occurredAt,
    String ip,
    String location,
    String device
) {}

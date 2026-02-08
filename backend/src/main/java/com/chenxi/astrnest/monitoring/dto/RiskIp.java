package com.chenxi.astrnest.monitoring.dto;

import java.time.Instant;

public record RiskIp(String ip, String reason, Instant lockedUntil, String dimension) {
}

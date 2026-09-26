package com.chenxi.astrnest.license.dto;

import java.time.Instant;
import java.util.Map;

/**
 * POST report-url 的请求体（N2 占位，协议只到接口层）。
 */
public record StatsReportRequest(String eventType, Instant reportedAt, Map<String, Object> payload) {
}

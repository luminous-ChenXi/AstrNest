package com.imgbed.monitoring.dto;

import java.util.List;

public record OverviewMetricsResponse(List<MetricCard> cards, ServerStatus serverStatus) {}

package com.chenxi.astrnest.monitoring.dto;

import java.util.List;

public record OverviewMetricsResponse(List<MetricCard> cards, ServerStatus serverStatus) {}

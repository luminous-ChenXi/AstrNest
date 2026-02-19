package com.chenxi.astrnest.monitoring.dto;

public record ServerStatus(double cpuUsage, double memoryUsage, long uptimeSeconds, String health) {}

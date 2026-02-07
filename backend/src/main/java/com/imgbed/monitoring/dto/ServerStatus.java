package com.imgbed.monitoring.dto;

public record ServerStatus(double cpuUsage, double memoryUsage, long uptimeSeconds, String health) {}

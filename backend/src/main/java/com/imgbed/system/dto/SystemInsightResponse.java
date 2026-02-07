package com.imgbed.system.dto;

public record SystemInsightResponse(
    long totalUsers,
    long usersWithEmail,
    long adminUsers,
    long totalUploads,
    long todayUploads,
    long totalStorageBytes,
    double totalStorageGigabytes,
    double emailCompletionRate
) {
}

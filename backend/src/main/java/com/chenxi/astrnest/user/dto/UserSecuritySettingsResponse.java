package com.chenxi.astrnest.user.dto;

import java.util.List;

public record UserSecuritySettingsResponse(
    String apiHeaderName,
    int defaultDailyQuota,
    List<LoginHistoryEntry> recentLogins
) {}

package com.chenxi.astrnest.monitoring.dto;

import java.util.List;

public record SecuritySummaryResponse(
    long loginFailLast24h,
    long loginFailTotal,
    long registerFailLast24h,
    List<TopCount> topLoginUsernames,
    List<TopCount> topRegisterUsernames,
    List<RiskIp> riskIps
) {
}

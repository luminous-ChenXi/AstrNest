package com.chenxi.astrnest.monitoring;

import com.chenxi.astrnest.monitoring.dto.OperationLogEntry;
import com.chenxi.astrnest.monitoring.dto.OverviewMetricsResponse;
import com.chenxi.astrnest.monitoring.dto.SecuritySummaryResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/monitor")
@RequiredArgsConstructor
public class MonitoringController {

  private final MonitoringService monitoringService;
  private final OperationLogService operationLogService;
  private final SecurityAnalyticsService securityAnalyticsService;

  @GetMapping("/overview")
  public OverviewMetricsResponse overview() {
    return monitoringService.snapshotOverview();
  }

  @GetMapping("/logs")
  public List<OperationLogEntry> logs() {
    return operationLogService.fetchRecentLogs();
  }

  @GetMapping("/security")
  public SecuritySummaryResponse security() {
    return securityAnalyticsService.summarize();
  }
}

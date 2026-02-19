package com.chenxi.astrnest.monitoring;

import com.chenxi.astrnest.monitoring.dto.MetricCard;
import com.chenxi.astrnest.monitoring.dto.OverviewMetricsResponse;
import com.chenxi.astrnest.monitoring.dto.ServerStatus;
import com.chenxi.astrnest.upload.record.UploadRecordRepository;
import com.sun.management.OperatingSystemMXBean;
import java.lang.management.ManagementFactory;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MonitoringService {

  private static final String ACCENT_PRIMARY = "from-brand-primary to-brand-accent";
  private static final String ACCENT_STORAGE = "from-brand-accent to-brand-emerald";
  private static final String ACCENT_ALERT = "from-rose-400 to-amber-400";

  private final UploadRecordRepository uploadRecordRepository;
  private final Runtime runtime = Runtime.getRuntime();
  private final Instant applicationStartedAt = Instant.now();
  private final OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
  private final NumberFormat numberFormat = NumberFormat.getInstance();

  public MonitoringService(UploadRecordRepository uploadRecordRepository) {
    this.uploadRecordRepository = uploadRecordRepository;
    this.numberFormat.setMaximumFractionDigits(1);
  }

  @Transactional(readOnly = true)
  public OverviewMetricsResponse snapshotOverview() {
    ZoneId zoneId = ZoneId.systemDefault();
    Instant startOfToday = LocalDate.now(zoneId).atStartOfDay(zoneId).toInstant();
    Instant startOfYesterday = startOfToday.minus(1, ChronoUnit.DAYS);

    long todayUploads = uploadRecordRepository.countUploadedAfter(startOfToday);
    long yesterdayUploads = uploadRecordRepository.countUploadedBetween(startOfYesterday, startOfToday);
    long totalStorageBytes = uploadRecordRepository.totalStorageBytes();
    long storedToday = uploadRecordRepository.totalSizeUploadedAfter(startOfToday);
    long violationCount = uploadRecordRepository.countByViolationTrue();
    long approvedToday = uploadRecordRepository.countByViolationFalseAndUploadedAtAfter(startOfToday);

    List<MetricCard> cards = List.of(
        new MetricCard("今日上传", formatNumber(todayUploads), formatPercentDelta(todayUploads, yesterdayUploads), ACCENT_PRIMARY),
        new MetricCard("总存储", formatBytes(totalStorageBytes), formatStorageDelta(storedToday), ACCENT_STORAGE),
        new MetricCard("违规告警", formatNumber(violationCount), formatReviewDelta(approvedToday), ACCENT_ALERT)
    );

    return new OverviewMetricsResponse(cards, captureServerStatus());
  }

  @SuppressWarnings("deprecation")
  public ServerStatus captureServerStatus() {
    double cpuUsage = -1;
    if (osBean != null) {
      double load = osBean.getSystemCpuLoad();
      if (load >= 0) {
        cpuUsage = load * 100;
      }
    }

    long maxMemory = runtime.maxMemory();
    long usedMemory = runtime.totalMemory() - runtime.freeMemory();
    double memoryUsage = maxMemory > 0 ? (double) usedMemory / maxMemory * 100 : -1;

    long uptimeSeconds = Duration.between(applicationStartedAt, Instant.now()).toSeconds();
    String health = evaluateHealth(cpuUsage, memoryUsage);

    return new ServerStatus(round(cpuUsage), round(memoryUsage), uptimeSeconds, health);
  }

  private String formatNumber(long value) {
    return numberFormat.format(value);
  }

  private String formatPercentDelta(long current, long previous) {
    if (previous <= 0) {
      return current > 0 ? "较昨日 +" + formatNumber(current) : "较昨日 0";
    }
    double percent = ((double) current - previous) / previous * 100;
    return (percent >= 0 ? "+" : "") + Math.round(percent) + "% vs 昨日";
  }

  private String formatStorageDelta(long addedBytes) {
    if (addedBytes <= 0) {
      return "今日新增 0";
    }
    return "今日 +" + formatBytes(addedBytes);
  }

  private String formatReviewDelta(long approvedToday) {
    if (approvedToday <= 0) {
      return "今日 0 条";
    }
    return "今日 +" + formatNumber(approvedToday);
  }

  private String formatBytes(long bytes) {
    if (bytes <= 0) {
      return "0 B";
    }
    double value = bytes;
    String[] units = {"B", "KB", "MB", "GB", "TB"};
    int index = 0;
    while (value >= 1024 && index < units.length - 1) {
      value /= 1024;
      index++;
    }
    return String.format(index == 0 ? "%.0f %s" : "%.1f %s", value, units[index]);
  }

  private String evaluateHealth(double cpuUsage, double memoryUsage) {
    if (cpuUsage < 0 || memoryUsage < 0) {
      return "UNKNOWN";
    }
    if (cpuUsage < 70 && memoryUsage < 70) {
      return "HEALTHY";
    }
    if (cpuUsage < 85 && memoryUsage < 85) {
      return "WARNING";
    }
    return "CRITICAL";
  }

  private double round(double value) {
    if (value < 0) {
      return value;
    }
    return Math.round(value * 10.0) / 10.0;
  }
}

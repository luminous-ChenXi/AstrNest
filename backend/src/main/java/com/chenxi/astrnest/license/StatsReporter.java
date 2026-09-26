package com.chenxi.astrnest.license;

import com.chenxi.astrnest.license.dto.StatsReportRequest;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

/**
 * 统计上报器（N2 占位骨架，统一规范第 3 项）。
 *
 * <p><b>{@code enabled=false}（默认）时为空实现</b>：{@link #report(String, Map)} 直接返回，
 * 定时任务不做任何事，不产生网络请求、不收集任何数据。</p>
 *
 * <p>开启后（N2 落地）每日一次向 report-url 汇总上报；report 协议只到接口层
 * （{@link StatsReportRequest} 为最小占位结构），上报失败静默忽略——
 * 统计永远不影响站点功能。</p>
 */
@Slf4j
@Component
public class StatsReporter {

  private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(5);

  private final ChenxiStatsProperties properties;
  private final RestClient restClient;

  public StatsReporter(ChenxiStatsProperties properties) {
    this.properties = properties;
    this.restClient = RestClient.builder()
        .requestFactory(newTimeoutFactory(HTTP_TIMEOUT))
        .build();
  }

  /** 每日心跳上报（enabled=false 时空转）。 */
  @Scheduled(initialDelay = 5 * 60 * 1000L, fixedDelay = 24 * 60 * 60 * 1000L)
  public void dailyReport() {
    if (!properties.isEnabled()) {
      return;
    }
    report("heartbeat", Map.of());
  }

  /**
   * 上报一条统计事件。未启用或上报失败时静默返回——统计链路的任何问题都不允许影响业务。
   *
   * @param eventType 事件类型（N2 协议确定取值集）
   * @param payload   事件负载（N2 协议确定字段；当前实现不采集任何用户内容）
   */
  public void report(String eventType, Map<String, Object> payload) {
    if (!properties.isEnabled()) {
      return;
    }
    if (!StringUtils.hasText(properties.getReportUrl())) {
      log.debug("chenxi.stats.report-url 未配置，跳过统计上报：{}", eventType);
      return;
    }
    try {
      restClient.post()
          .uri(properties.getReportUrl().trim())
          .contentType(MediaType.APPLICATION_JSON)
          .body(new StatsReportRequest(eventType, Instant.now(), payload == null ? Map.of() : payload))
          .retrieve()
          .toBodilessEntity();
    } catch (Exception exception) {
      log.debug("统计上报失败（静默忽略）：eventType={}, error={}", eventType, exception.getMessage());
    }
  }

  private static org.springframework.http.client.SimpleClientHttpRequestFactory newTimeoutFactory(Duration timeout) {
    var requestFactory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(timeout);
    requestFactory.setReadTimeout(timeout);
    return requestFactory;
  }
}

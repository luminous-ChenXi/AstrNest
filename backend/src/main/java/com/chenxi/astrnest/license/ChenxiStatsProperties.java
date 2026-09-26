package com.chenxi.astrnest.license;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 统计上报配置（{@code chenxi.stats.*}，统一规范命名空间，N2 占位）。
 *
 * <p><b>默认关闭</b>（{@code enabled=false}）：关闭时 {@link StatsReporter} 为空实现，
 * 不产生任何网络请求与数据收集。report 协议只到接口层，N2 阶段确定。</p>
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "chenxi.stats")
public class ChenxiStatsProperties {

  /** 统计上报总开关，默认 false。 */
  private boolean enabled = false;

  /** 统计上报端点（N2 占位）。 */
  private String reportUrl = "";
}

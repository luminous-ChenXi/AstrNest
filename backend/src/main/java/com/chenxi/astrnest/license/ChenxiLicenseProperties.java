package com.chenxi.astrnest.license;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 正版授权校验配置（{@code chenxi.license.*}，统一规范命名空间，N2 占位）。
 *
 * <p><b>默认关闭</b>（{@code enabled=false}）：关闭时 {@link LicenseClient} 不发起任何网络请求，
 * 状态恒为 {@link LicenseState#DISABLED}，站点功能完全不受影响。</p>
 *
 * <p>N2 落地口径：授权按年计、由"辰汐授权服务"签发；与通行证令牌的 30 天滑动过期
 * 是两套独立生命周期（解耦设计，见 docs/chenxi-integration.md）。verify 协议目前只到接口层，
 * 服务端与最终报文格式在 N2 阶段确定。</p>
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "chenxi.license")
public class ChenxiLicenseProperties {

  /** 正版授权校验总开关，默认 false。 */
  private boolean enabled = false;

  /** 授权校验端点（N2 占位；为空时即使 enabled=true 也不发起请求，按离线宽限处理）。 */
  private String verifyUrl = "";

  /** 校验结果的本地缓存天数：缓存期内不再请求 verify-url。默认 7 天。 */
  private int cacheDays = 7;

  /** 离线宽限天数：verify 不可达时，自上次校验成功起在该天数内放行。默认 3 天。 */
  private int offlineGraceDays = 3;
}

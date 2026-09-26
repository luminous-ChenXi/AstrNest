package com.chenxi.astrnest.license;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 授权状态公开端点（只读、无敏感信息）。
 *
 * <p>默认关闭时返回 {@code enabled=false}，前端不渲染任何授权 UI；开启且超离线宽限时
 * 返回 {@code needsBanner=true}，前端展示横幅提醒站长完成授权——<b>仅提示，绝不锁数据</b>。
 * 未安装阶段由 InstallGuardFilter 放行（不依赖业务表）。</p>
 */
@RestController
@RequestMapping("/api/license")
@RequiredArgsConstructor
public class LicenseStatusController {

  private final LicenseClient licenseClient;

  @GetMapping("/status")
  public LicenseStatusResponse status() {
    return licenseClient.currentStatus();
  }
}

package com.chenxi.astrnest.system;

import com.chenxi.astrnest.install.InstallStatusService;
import com.chenxi.astrnest.system.dto.PublicSystemConfigResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system/public-config")
@RequiredArgsConstructor
public class SystemPublicConfigController {

  private final SystemConfigService systemConfigService;
  private final InstallStatusService installStatusService;

  @GetMapping
  public PublicSystemConfigResponse load() {
    // 短路保护：schema 未安装（NOT_INSTALLED/EMPTY）时不能查询 system_config 表，
    // 否则 JPA 会因表不存在抛 500。返回仅含 installed:false 的最小响应供前端进入安装向导。
    if (!installStatusService.isInstalled()) {
      return PublicSystemConfigResponse.uninstalled();
    }
    PublicSystemConfigResponse config = systemConfigService.getPublicConfig();
    return new PublicSystemConfigResponse(
        config.customFooterHtml(),
        config.autoCleanupDays(),
        config.assetDomain(),
        config.maxFilesPerUpload(),
        config.maxUploadMegabytes(),
        config.maxVideoUploadMegabytes(),
        config.videoChunkUploadEnabled(),
        config.videoChunkSizeMb(),
        config.guestUploadEnabled(),
        config.registrationEnabled(),
        config.emailVerifyRequired(),
        true
    );
  }
}

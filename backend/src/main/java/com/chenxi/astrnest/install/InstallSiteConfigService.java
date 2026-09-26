package com.chenxi.astrnest.install;

import com.chenxi.astrnest.system.SystemConfig;
import com.chenxi.astrnest.system.SystemConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/**
 * 安装向导「站点配置」步骤：把初始站点开关写入 system_config 单行配置。
 *
 * <p>仅在数据库初始化完成后可调用（system_config 表此时才存在），全部字段可选——
 * 为 null 的字段保持系统默认，不做覆盖。与安装完成后的 SystemConfigService.updateConfig
 * 相比只开放"面向公众"的少量开关，AI/存储等高级配置仍引导去管理后台。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InstallSiteConfigService {

  private static final long BYTES_PER_MB = 1024L * 1024L;

  private final SystemConfigRepository systemConfigRepository;

  @Transactional
  public void apply(InstallSiteConfigRequest request) {
    SystemConfig config = systemConfigRepository.findById(1L)
        .orElseGet(() -> systemConfigRepository.save(new SystemConfig()));

    if (request.registrationEnabled() != null) {
      config.setRegistrationEnabled(request.registrationEnabled());
    }
    if (request.guestUploadEnabled() != null) {
      config.setGuestUploadEnabled(request.guestUploadEnabled());
    }
    if (request.maxUploadMb() != null) {
      config.setMaxUploadBytes(request.maxUploadMb() * BYTES_PER_MB);
    }
    if (request.assetDomain() != null) {
      config.setAssetDomain(normalizeDomain(request.assetDomain()));
    }
    if (request.customFooterHtml() != null) {
      String footer = request.customFooterHtml().trim();
      config.setCustomFooterHtml(footer.isEmpty() ? null : footer);
    }
    config.setUpdatedBy("install-wizard");
    systemConfigRepository.save(config);
    log.info("Install wizard: site config applied (registration={}, guestUpload={}, maxUploadMb={}, assetDomain={})",
        config.isRegistrationEnabled(), config.isGuestUploadEnabled(),
        request.maxUploadMb(), StringUtils.hasText(config.getAssetDomain()) ? "set" : "unset");
  }

  private String normalizeDomain(String domain) {
    String normalized = domain.trim();
    if (normalized.isEmpty()) {
      return null;
    }
    if (!normalized.startsWith("http://") && !normalized.startsWith("https://")) {
      normalized = "https://" + normalized;
    }
    while (normalized.length() > 1 && normalized.endsWith("/")) {
      normalized = normalized.substring(0, normalized.length() - 1);
    }
    return normalized;
  }
}

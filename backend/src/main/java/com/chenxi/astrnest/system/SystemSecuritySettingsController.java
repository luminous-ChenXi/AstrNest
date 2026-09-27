package com.chenxi.astrnest.system;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端「注册与登录安全」设置：站长级两个开关 + SMTP 就绪状态。
 *
 * <p>仅 ADMIN 可读写（双保险：路由前缀 /api/admin/** 已限 ADMIN，这里再加方法级注解）。
 * 开关独立成端点而非走全量 system-config，避免保存两个布尔值时被迫携带并校验
 * 上传限额等全部字段。</p>
 */
@RestController
@RequestMapping("/api/admin/security-settings")
@RequiredArgsConstructor
public class SystemSecuritySettingsController {

  private final SystemConfigService systemConfigService;

  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  public SecuritySettingsResponse load() {
    return new SecuritySettingsResponse(
        systemConfigService.isEmailVerifyRequired(),
        systemConfigService.isTotpRequired(),
        systemConfigService.isSmtpConfigured());
  }

  @PutMapping
  @PreAuthorize("hasRole('ADMIN')")
  public SecuritySettingsResponse update(@Valid @RequestBody UpdateSecuritySettingsRequest request) {
    systemConfigService.updateSecuritySwitches(request.emailVerifyRequired(), request.totpRequired());
    return new SecuritySettingsResponse(
        systemConfigService.isEmailVerifyRequired(),
        systemConfigService.isTotpRequired(),
        systemConfigService.isSmtpConfigured());
  }

  /**
   * 站长安全开关集。
   *
   * @param emailVerifyRequired 注册邮箱验证开关（开启前需先配置 SMTP，否则验证码无法发送）
   * @param totpRequired        登录二步验证（TOTP）强制开关
   * @param smtpConfigured      SMTP 是否已配置并启用（只读，驱动前端警告文案）
   */
  public record SecuritySettingsResponse(boolean emailVerifyRequired, boolean totpRequired, boolean smtpConfigured) {
  }

  public record UpdateSecuritySettingsRequest(Boolean emailVerifyRequired, Boolean totpRequired) {
  }
}

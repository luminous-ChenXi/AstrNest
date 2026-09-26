package com.chenxi.astrnest.license;

import java.time.Instant;

/**
 * GET /api/license/status 响应：面向前端的授权状态摘要（公开只读，无敏感信息）。
 *
 * @param enabled        授权校验是否启用
 * @param state          当前状态（DISABLED/ACTIVE/GRACE/OVERDUE/INVALID）
 * @param needsBanner    是否需要前端横幅提示（超离线宽限或明确无效；仅提示绝不锁数据）
 * @param lastVerifiedAt 最近一次 verify 成功时间（可空）
 * @param expiresAt      授权到期时间（N2 协议确定前恒为空）
 * @param message        人类可读说明
 */
public record LicenseStatusResponse(
    boolean enabled,
    LicenseState state,
    boolean needsBanner,
    Instant lastVerifiedAt,
    Instant expiresAt,
    String message) {

  /** 关闭态：enabled=false，前端不展示任何授权相关 UI。 */
  public static LicenseStatusResponse disabled() {
    return new LicenseStatusResponse(
        false, LicenseState.DISABLED, false, null, null, "正版授权校验未启用");
  }
}

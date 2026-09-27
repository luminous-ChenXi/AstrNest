package com.chenxi.astrnest.install;

/**
 * POST /api/install/finish 响应：完成标记写入结果 + 安装汇总（完成页展示用）。
 *
 * @param success      是否写入成功
 * @param installedAt  完成时间
 * @param message      结果消息
 * @param summary      安装汇总（管理员账号摘要 + 站点开关状态；均为完成时刻的快照，不含任何密码）
 */
public record InstallFinishResponse(boolean success, String installedAt, String message, InstallSummary summary) {

  /**
   * 完成汇总：站点地址由前端按当前访问 origin 展示，这里只回传后端已知的部分。
   *
   * @param adminUsername        初始管理员用户名（最早的 ADMIN 账号）
   * @param adminEmail           初始管理员邮箱（可为 null）
   * @param emailVerifyRequired  注册邮箱验证开关当前状态
   * @param totpRequired         登录二步验证（TOTP）开关当前状态
   * @param smtpConfigured       SMTP 邮件服务是否已配置并启用（影响邮箱验证可用性）
   */
  public record InstallSummary(
      String adminUsername,
      String adminEmail,
      boolean emailVerifyRequired,
      boolean totpRequired,
      boolean smtpConfigured) {
  }
}

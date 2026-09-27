package com.chenxi.astrnest.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * POST /api/auth/2fa/verify 与 /api/auth/2fa/setup/confirm 请求体。
 *
 * @param tempToken 登录时下发的 5 分钟过渡令牌（purpose=2fa）
 * @param code      6 位 TOTP 动态码，或 8 位一次性还原码（仅 verify 接受还原码）
 */
public record TwoFactorVerifyRequest(
    @NotBlank(message = "缺少二步验证凭证") String tempToken,
    @NotBlank(message = "请输入验证码") @Size(max = 8, message = "验证码为 6 位动态码或 8 位还原码") String code) {
}

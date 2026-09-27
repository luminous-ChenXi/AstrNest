package com.chenxi.astrnest.user.dto;

/**
 * 登录二步验证挑战信息（随 LoginResponse.mode 返回）。
 *
 * @param tempToken   5 分钟有效过渡令牌（purpose=2fa 的 JWT，不可作访问令牌）
 * @param otpauthUri  otpauth://totp/...（仅 TOTP_SETUP 模式返回，前端渲染二维码）
 * @param secret      Base32 密钥（仅 TOTP_SETUP 模式返回，供无法扫码时手工录入）
 * @param username    挑战对应的用户名（前端展示用）
 */
public record TwoFactorChallenge(String tempToken, String otpauthUri, String secret, String username) {
}

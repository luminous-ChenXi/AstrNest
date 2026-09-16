package com.chenxi.astrnest.passport.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * SSO 登录换取本地 JWT 的请求体：前端在身份源侧完成授权码 + PKCE 换取 access_token 后，
 * 将 opaque access_token 提交到本地换发与本地登录一致的 JWT。
 */
public record SsoExchangeRequest(
    @NotBlank(message = "accessToken 不能为空") String accessToken
) {}

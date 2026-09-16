package com.chenxi.astrnest.passport.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 身份源 userinfo 响应（{@code GET {issuer}/userinfo}，Bearer token）。
 *
 * <p>所有字段均可空容忍：不同身份源返回的 claims 略有差异，缺失时影子账号自动降级占位。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SsoUserInfo(
    String sub,
    String username,
    String nickname,
    String avatar,
    String email
) {}

package com.chenxi.astrnest.passport.dto;

import java.util.List;

/**
 * SSO 登录配置（公开接口返回，供前端决定是否展示登录入口并拼接授权地址）。
 *
 * @param enabled           是否启用（关闭时前端隐藏 SSO 入口，其余字段为 null）
 * @param issuer            身份源签发方地址
 * @param clientId          public client id
 * @param scopes            申请的 scope 列表
 * @param redirectUri       授权回调地址（须与身份源侧注册一致）
 * @param authorizeEndpoint 授权端点（issuer + /oauth2/authorize）
 * @param tokenEndpoint     token 端点（issuer + /oauth2/token）
 */
public record SsoConfigResponse(
    boolean enabled,
    String issuer,
    String clientId,
    List<String> scopes,
    String redirectUri,
    String authorizeEndpoint,
    String tokenEndpoint
) {

  /** 关闭态响应：仅返回 enabled=false，前端据此隐藏入口。 */
  public static SsoConfigResponse disabled() {
    return new SsoConfigResponse(false, null, null, List.of(), null, null, null);
  }
}

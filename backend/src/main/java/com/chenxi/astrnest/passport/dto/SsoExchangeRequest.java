package com.chenxi.astrnest.passport.dto;

/**
 * 通行证登录换取本地 JWT 的请求体（两种方式二选一）：
 *
 * <ul>
 *   <li><b>OIDC 授权码 + PKCE（推荐）</b>：{@code code} + {@code codeVerifier}；
 *       后端向 {@code {issuer}/oauth2/token} 完成换 token（public client，无 client_secret），
 *       浏览器全程不接触通行证 token；</li>
 *   <li><b>遗留 access_token 直换</b>：{@code accessToken}——前端自行完成授权码换 token 后提交
 *       opaque token，由后端回站自省校验。保留以兼容旧版前端。</li>
 * </ul>
 */
public record SsoExchangeRequest(String code, String codeVerifier, String accessToken) {

  /** 请求是否包含可处理的凭证（授权码对 或 遗留 access_token）。 */
  public boolean hasCredential() {
    return (code != null && !code.isBlank() && codeVerifier != null && !codeVerifier.isBlank())
        || (accessToken != null && !accessToken.isBlank());
  }
}

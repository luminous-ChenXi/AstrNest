package com.chenxi.astrnest.passport;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * SSO 外部身份源配置（{@code astrnest.sso.*}）。
 *
 * <p>用于对接任何兼容 OAuth 2.1 / OIDC「授权码 + PKCE」的身份源（如"辰汐通行证"）。
 * <b>默认关闭</b>（{@code enabled=false}），关闭时对现有本地登录/注册/API Key 零影响。
 *
 * <p>端点约定（与主站通行证生态相似兼容）：{@code {issuer}/oauth2/authorize}、
 * {@code {issuer}/oauth2/token}、{@code {issuer}/oauth2/introspect}、{@code {issuer}/userinfo}。
 * 深度对接（discovery 元数据、id_token 校验、token 刷新等）由项目作者后期扩展，
 * 扩展点集中在 {@link PassportClient} 与 {@link SsoIdentityService}。
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "astrnest.sso")
public class SsoProperties {

  /** 是否启用 SSO 登录，默认 false（关闭时前端隐藏入口、后端 exchange 返回明确错误）。 */
  private boolean enabled = false;

  /** 身份源签发方地址（issuer），如 https://passport.example.com，末尾斜杠会被容忍。 */
  private String issuer;

  /** 在身份源侧注册的 public client id（OAuth 2.1 推荐公共客户端，无 client_secret）。 */
  private String clientId;

  /** 授权请求申请的 scope，默认 openid/profile/email。 */
  private List<String> scopes = List.of("openid", "profile", "email");

  /** 授权完成后的回调地址，须与身份源侧注册的 redirect_uri 完全一致。 */
  private String redirectUri;

  /** token 自省（introspect）请求超时秒数。 */
  private int introspectTimeoutSeconds = 5;

  /** userinfo 请求超时秒数。 */
  private int userinfoTimeoutSeconds = 5;
}

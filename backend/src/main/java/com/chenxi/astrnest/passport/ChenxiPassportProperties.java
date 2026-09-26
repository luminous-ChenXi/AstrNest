package com.chenxi.astrnest.passport;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 辰汐通行证登录配置（{@code chenxi.passport.*}，统一规范命名空间）。
 *
 * <p>用于对接任何兼容 OAuth 2.1 / OIDC「授权码 + PKCE(S256)」的身份源（如"辰汐通行证"）。
 * <b>默认关闭</b>（{@code enabled=false}），关闭时对现有本地登录/注册/API Key 零影响：
 * 前端隐藏入口、exchange 接口返回明确错误。</p>
 *
 * <p>端点约定（与主站通行证生态相似兼容）：{@code {issuer}/oauth2/authorize}、
 * {@code {issuer}/oauth2/token}、{@code {issuer}/oauth2/introspect}、{@code {issuer}/userinfo}。</p>
 *
 * <p>令牌口径与授权口径<b>解耦</b>：本地会话 JWT 的不活动过期由 {@link #accessTokenDays}
 * 控制（滑动刷新，每次携带有效令牌访问自动续期）；而通行证侧的授权（license）按年计，
 * 两者互不影响（N2 落地，见 license 包）。</p>
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "chenxi.passport")
public class ChenxiPassportProperties {

  /** 是否启用通行证登录，默认 false（关闭时前端隐藏入口、后端 exchange 返回明确错误）。 */
  private boolean enabled = false;

  /** 身份源签发方地址（issuer），如 https://passport.example.com，末尾斜杠会被容忍。 */
  private String issuer;

  /** 在身份源侧注册的 public client id（OAuth 2.1 推荐公共客户端，无 client_secret）。 */
  private String clientId;

  /** 授权回调地址，须与身份源侧注册的 redirect_uri 完全一致。 */
  private String redirectUri;

  /** 授权请求申请的 scope，统一规范默认 openid,profile。 */
  private List<String> scopes = List.of("openid", "profile");

  /**
   * 本地令牌不活动过期天数（滑动刷新）：签发的本地 JWT 有效期即该天数，
   * 且每次携带有效令牌的请求都会通过响应头滑动续期。统一规范默认 30 天。
   */
  private int accessTokenDays = 30;

  /** token 自省（introspect）请求超时秒数。 */
  private int introspectTimeoutSeconds = 5;

  /** userinfo 请求超时秒数。 */
  private int userinfoTimeoutSeconds = 5;

  /** 授权码换 token（/oauth2/token）请求超时秒数。 */
  private int tokenTimeoutSeconds = 5;
}

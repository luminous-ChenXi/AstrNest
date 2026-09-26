package com.chenxi.astrnest.passport;

import com.chenxi.astrnest.passport.dto.IntrospectionResult;
import com.chenxi.astrnest.passport.dto.PassportToken;
import com.chenxi.astrnest.passport.dto.SsoUserInfo;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

/**
 * 辰汐通行证 HTTP 客户端（基于 Spring RestClient）。
 *
 * <p>负责三个最小必要调用：
 * <ul>
 *   <li>{@link #exchangeAuthorizationCode(String, String)}：POST {@code {issuer}/oauth2/token}
 *       （grant_type=authorization_code + PKCE code_verifier，public client 无 client_secret），
 *       OIDC 授权码 + PKCE 流程的服务端换 token 步骤；</li>
 *   <li>{@link #introspect(String)}：POST {@code {issuer}/oauth2/introspect}（RFC 7662，form：token + client_id），
 *       access_token 为 opaque token 时必须回站自省校验；任何 HTTP 错误/超时一律按 active=false 处理（fail-closed）；</li>
 *   <li>{@link #fetchUserInfo(String)}：GET {@code {issuer}/userinfo}（Bearer token），拉取用户 claims。</li>
 * </ul>
 *
 * <p>通行证侧 access_token 用完即弃（不缓存、不刷新）：本站会话由本地 JWT 承载，
 * 不活动过期由 {@code chenxi.passport.access-token-days} 滑动刷新，与通行证授权完全解耦。</p>
 */
@Slf4j
@Component
public class PassportClient {

  private final ChenxiPassportProperties properties;
  private final RestClient tokenClient;
  private final RestClient introspectClient;
  private final RestClient userinfoClient;

  public PassportClient(ChenxiPassportProperties properties) {
    this.properties = properties;
    this.tokenClient = buildClient(Duration.ofSeconds(properties.getTokenTimeoutSeconds()));
    this.introspectClient = buildClient(Duration.ofSeconds(properties.getIntrospectTimeoutSeconds()));
    this.userinfoClient = buildClient(Duration.ofSeconds(properties.getUserinfoTimeoutSeconds()));
  }

  /**
   * OIDC 授权码 + PKCE：凭 code + code_verifier 向身份源换取 access_token。
   * 公共客户端不携带 client_secret；任何失败统一按 401 语义抛出（宁可拒绝不可放行）。
   */
  public PassportToken exchangeAuthorizationCode(String code, String codeVerifier) {
    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("grant_type", "authorization_code");
    form.add("client_id", properties.getClientId());
    form.add("code", code);
    form.add("redirect_uri", StringUtils.hasText(properties.getRedirectUri())
        ? properties.getRedirectUri().trim() : "");
    form.add("code_verifier", codeVerifier);
    try {
      PassportToken token = tokenClient.post()
          .uri(endpoint("/oauth2/token"))
          .contentType(MediaType.APPLICATION_FORM_URLENCODED)
          .body(form)
          .retrieve()
          .body(PassportToken.class);
      if (token == null || !StringUtils.hasText(token.accessToken())) {
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "通行证授权码换取失败：未返回 access_token");
      }
      return token;
    } catch (ResponseStatusException exception) {
      throw exception;
    } catch (Exception exception) {
      log.warn("通行证授权码换 token 失败：issuer={}, error={}", properties.getIssuer(), exception.getMessage());
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "通行证授权码无效或已过期，请重新登录");
    }
  }

  /**
   * 自省 access_token。无效 token 身份源返回 {@code {"active":false}}（HTTP 200）；
   * 网络/超时/解析异常统一降级为 active=false 并打 WARN 日志（凭证校验宁可拒绝不可放行）。
   */
  public IntrospectionResult introspect(String accessToken) {
    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("token", accessToken);
    form.add("client_id", properties.getClientId());
    try {
      IntrospectionResult result = introspectClient.post()
          .uri(endpoint("/oauth2/introspect"))
          .contentType(MediaType.APPLICATION_FORM_URLENCODED)
          .body(form)
          .retrieve()
          .body(IntrospectionResult.class);
      return result == null ? IntrospectionResult.inactive() : result;
    } catch (Exception ex) {
      log.warn("SSO token 自省失败，按无效凭证处理：issuer={}, error={}", properties.getIssuer(), ex.getMessage());
      return IntrospectionResult.inactive();
    }
  }

  /**
   * 拉取用户信息。失败时抛 401 语义异常，由全局异常处理器返回统一错误体。
   */
  public SsoUserInfo fetchUserInfo(String accessToken) {
    try {
      SsoUserInfo userInfo = userinfoClient.get()
          .uri(endpoint("/userinfo"))
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
          .retrieve()
          .body(SsoUserInfo.class);
      if (userInfo == null || !StringUtils.hasText(userInfo.sub())) {
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "SSO 用户信息缺失，凭证无效");
      }
      return userInfo;
    } catch (ResponseStatusException ex) {
      throw ex;
    } catch (Exception ex) {
      log.warn("SSO 用户信息获取失败：issuer={}, error={}", properties.getIssuer(), ex.getMessage());
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "SSO 用户信息获取失败，请重新登录");
    }
  }

  /** 拼接身份源端点（容忍 issuer 末尾斜杠）。 */
  private String endpoint(String path) {
    String issuer = StringUtils.hasText(properties.getIssuer())
        ? properties.getIssuer().trim()
        : "";
    return issuer.endsWith("/") ? issuer.substring(0, issuer.length() - 1) + path : issuer + path;
  }

  private static RestClient buildClient(Duration timeout) {
    // 连接/读取超时一致，避免身份源抖动拖垮登录线程
    var requestFactory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(timeout);
    requestFactory.setReadTimeout(timeout);
    return RestClient.builder()
        .requestFactory(requestFactory)
        .build();
  }
}

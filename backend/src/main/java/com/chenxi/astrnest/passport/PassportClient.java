package com.chenxi.astrnest.passport;

import com.chenxi.astrnest.passport.dto.IntrospectionResult;
import com.chenxi.astrnest.passport.dto.SsoUserInfo;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

/**
 * 身份源 HTTP 客户端（基于 Spring RestClient）。
 *
 * <p>负责两个最小必要调用：
 * <ul>
 *   <li>{@link #introspect(String)}：POST {@code {issuer}/oauth2/introspect}（RFC 7662，form：token + client_id），
 *       access_token 为 opaque token 时必须回站自省校验；任何 HTTP 错误/超时一律按 active=false 处理（fail-closed）。</li>
 *   <li>{@link #fetchUserInfo(String)}：GET {@code {issuer}/userinfo}（Bearer token），拉取用户 claims。</li>
 * </ul>
 *
 * <p>深度对接扩展点：后续如需 OIDC discovery（{@code {issuer}/.well-known/openid-configuration}）、
 * id_token(JWKS) 校验、refresh_token 管理等，建议在本类内扩展方法或新增同包客户端，保持调用面集中。
 */
@Slf4j
@Component
public class PassportClient {

  private final SsoProperties properties;
  private final RestClient introspectClient;
  private final RestClient userinfoClient;

  public PassportClient(SsoProperties properties) {
    this.properties = properties;
    this.introspectClient = buildClient(Duration.ofSeconds(properties.getIntrospectTimeoutSeconds()));
    this.userinfoClient = buildClient(Duration.ofSeconds(properties.getUserinfoTimeoutSeconds()));
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

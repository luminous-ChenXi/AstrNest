package com.chenxi.astrnest.passport.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * RFC 7662 token 自省响应（精简版）。
 *
 * <p>无效 token 时身份源统一返回 {@code {"active":false}} 且 HTTP 200。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record IntrospectionResult(
    Boolean active,
    String sub,
    String scope,
    String username
) {

  /** 统一的"未激活"结果：网络错误、超时、响应缺失字段时兜底使用。 */
  public static IntrospectionResult inactive() {
    return new IntrospectionResult(false, null, null, null);
  }
}

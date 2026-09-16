package com.chenxi.astrnest.common;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 统一的客户端 IP 解析器。
 *
 * <p>{@code astrnest.security.trusted-proxy=false}（默认）：直接返回 {@code request.getRemoteAddr()}，
 * 不信任任何请求头，防止伪造 X-Forwarded-For 绕过限流/配额。
 *
 * <p>{@code astrnest.security.trusted-proxy=true}：部署在可信反向代理（nginx 等）之后时，
 * 优先取 {@code X-Real-IP}，其次取 {@code X-Forwarded-For} 的第一个值，都没有则回退 {@code remoteAddr}。
 */
@Component
public class ClientIpResolver {

  private static final String UNKNOWN = "unknown";

  private final boolean trustedProxy;

  public ClientIpResolver(
      @Value("${astrnest.security.trusted-proxy:false}") boolean trustedProxy) {
    this.trustedProxy = trustedProxy;
  }

  public String resolve(HttpServletRequest request) {
    if (request == null) {
      return UNKNOWN;
    }
    if (!trustedProxy) {
      return remoteAddr(request);
    }
    String realIp = firstValue(request.getHeader("X-Real-IP"));
    if (realIp != null) {
      return realIp;
    }
    String forwarded = request.getHeader("X-Forwarded-For");
    if (StringUtils.hasText(forwarded)) {
      String first = forwarded.split(",")[0].trim();
      if (StringUtils.hasText(first)) {
        return first;
      }
    }
    return remoteAddr(request);
  }

  private String remoteAddr(HttpServletRequest request) {
    String remoteAddr = request.getRemoteAddr();
    return StringUtils.hasText(remoteAddr) ? remoteAddr : UNKNOWN;
  }

  private String firstValue(String headerValue) {
    if (!StringUtils.hasText(headerValue)) {
      return null;
    }
    String trimmed = headerValue.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}

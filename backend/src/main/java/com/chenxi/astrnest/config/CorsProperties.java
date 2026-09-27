package com.chenxi.astrnest.config;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "astrnest.cors")
public class CorsProperties {

  // 默认不放行任何跨域来源（安全默认）：部署时必须显式配置 astrnest.cors.allowed-origins，
  // 例如 https://your-domain.com。内网开发源也请通过本地配置自行添加。
  private List<String> allowedOrigins = List.of();

  private List<String> allowedMethods = List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");

  private List<String> allowedHeaders = List.of("*");

  // exposedHeaders 含 JWT 滑动续期响应头（JwtAuthenticationFilter，chenxi.passport.access-token-days 语义），
  // 保证跨域部署时前端也能读到刷新 token
  private List<String> exposedHeaders = List.of(
      "Content-Disposition",
      "X-RateLimit-Remaining",
      "X-RateLimit-Reset",
      "X-AstrNest-Refreshed-Token"
  );

  private boolean allowCredentials = true;

  private long maxAge = 3600L;
}

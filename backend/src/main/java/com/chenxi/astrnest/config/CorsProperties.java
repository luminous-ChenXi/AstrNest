package com.chenxi.astrnest.config;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "astrnest.cors")
public class CorsProperties {

  private List<String> allowedOrigins = List.of(
      "http://localhost:5175",
      "http://127.0.0.1:5175",
      "http://192.168.1.100:5175",
      "http://192.168.1.200:5175",
      "https://luminouschenxi.net",
      "https://www.luminouschenxi.net"
  );

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

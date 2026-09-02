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
      "http://localhost:5173",
      "http://127.0.0.1:5173",
      "http://192.168.1.100:5173",
      "http://192.168.1.200:5173",
      "https://luminouschenxi.net",
      "https://www.luminouschenxi.net"
  );

  private List<String> allowedMethods = List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");

  private List<String> allowedHeaders = List.of("*");

  private List<String> exposedHeaders = List.of("Content-Disposition", "X-RateLimit-Remaining", "X-RateLimit-Reset");

  private boolean allowCredentials = true;

  private long maxAge = 3600L;
}

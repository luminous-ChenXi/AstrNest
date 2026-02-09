package com.chenxi.astrnest.config;

import java.util.Objects;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

  private final CorsProperties corsProperties;

  public WebConfig(CorsProperties corsProperties) {
    this.corsProperties = corsProperties;
  }

  @Override
  public void addCorsMappings(@NonNull CorsRegistry registry) {
    registry.addMapping("/api/**")
        .allowedOrigins(toArray(corsProperties.getAllowedOrigins()))
        .allowedMethods(toArray(corsProperties.getAllowedMethods()))
        .allowedHeaders(toArray(corsProperties.getAllowedHeaders()))
        .exposedHeaders(toArray(corsProperties.getExposedHeaders()))
        .allowCredentials(corsProperties.isAllowCredentials())
        .maxAge(corsProperties.getMaxAge());
  }

  private @NonNull String[] toArray(java.util.List<String> values) {
    if (values == null || values.isEmpty()) {
      return new String[0];
    }
    return values.stream().filter(Objects::nonNull).toArray(String[]::new);
  }
}

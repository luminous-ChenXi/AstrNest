package com.chenxi.astrnest.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class UploadStaticResourceConfig implements WebMvcConfigurer {

  private final StorageProperties storageProperties;

  @Override
  public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
    String publicBase = storageProperties.getLocal().getPublicBaseUrl();
    if (!StringUtils.hasText(publicBase)) {
      publicBase = "/upload";
    }
    if (publicBase.startsWith("http")) {
      return;
    }
    String pattern = publicBase.startsWith("/") ? publicBase : "/" + publicBase;
    if (!pattern.endsWith("/**")) {
      pattern = pattern.endsWith("/") ? pattern + "**" : pattern + "/**";
    }

    String location = storageProperties.getLocal().resolvedRoot().toAbsolutePath().normalize().toUri().toString();
    if (!location.endsWith("/")) {
      location = location + "/";
    }
    registry.addResourceHandler(pattern).addResourceLocations(location);
  }
}

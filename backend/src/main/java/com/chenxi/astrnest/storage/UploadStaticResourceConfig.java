package com.chenxi.astrnest.storage;

import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.lang.NonNull;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

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
    String locationPrefix = location;
    registry.addResourceHandler(pattern)
        .addResourceLocations(location)
        .resourceChain(true)
        .addResolver(new PathResourceResolver() {
          @Override
          protected Resource getResource(String resourcePath, Resource location) throws IOException {
            Resource resolved = super.getResource(resourcePath, location);
            if (resolved == null) {
              return null;
            }
            // 显式校验：解析后的资源必须仍位于配置的存储根目录内，否则按 404 处理
            if (!resolved.getURL().toString().startsWith(locationPrefix)) {
              return null;
            }
            return resolved;
          }
        });
  }
}

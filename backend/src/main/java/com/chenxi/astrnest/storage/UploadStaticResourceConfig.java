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
import org.springframework.web.servlet.resource.ResourceHandlerUtils;

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
    registry.addResourceHandler(pattern)
        .addResourceLocations(location)
        // 不启用 CachingResourceResolver：它只按 resourcePath 缓存 Resource 句柄，
        // 文件被删除（管理端/用户端删除、到期清理）后会继续返回失效句柄，
        // 导致 lastModified() 抛 FileNotFoundException → 500，而非预期的 404。
        // 本地磁盘解析成本极低，每次重新 resolve 即可；浏览器缓存（ETag/Cache-Control）不受影响。
        .resourceChain(false)
        .addResolver(new StorageRootGuardResolver());
  }

  /**
   * 显式校验：解析后的资源必须仍位于配置的存储根目录内，否则按 404 处理。
   *
   * <p>判定必须走 Spring 统一口径（{@link ResourceHandlerUtils#isResourceUnderLocation}，
   * 与 {@link PathResourceResolver#checkResource} 内部一致）。location 字符串形如
   * {@code file:///D:/...} 而 {@code resolved.getURL().toString()} 形如 {@code file:/D:/...}，
   * 直接做字符串前缀比较永远为 false，会导致存在的文件也被判为越界而 404/500。</p>
   */
  static class StorageRootGuardResolver extends PathResourceResolver {

    @Override
    protected Resource getResource(String resourcePath, Resource location) throws IOException {
      Resource resolved = super.getResource(resourcePath, location);
      if (resolved == null) {
        return null;
      }
      if (!ResourceHandlerUtils.isResourceUnderLocation(location, resolved)) {
        return null;
      }
      return resolved;
    }
  }
}

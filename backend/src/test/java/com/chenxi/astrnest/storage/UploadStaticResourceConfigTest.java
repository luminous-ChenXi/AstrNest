package com.chenxi.astrnest.storage;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.handler.AbstractHandlerMapping;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;

/**
 * {@code /upload/**} 本地静态资源守卫（StorageRootGuardResolver）回归测试。
 *
 * <p>历史缺陷：守卫用字符串前缀比较判定「是否在存储根目录内」，但 location 字符串形如
 * {@code file:///D:/...}（Path.toUri().toString()），而 resolved.getURL().toString() 形如
 * {@code file:/D:/...}，前缀永远匹配不上，存在的文件也被判为越界 → NoResourceFoundException → 500。
 * 修复后改用 Spring 统一口径 {@code ResourceHandlerUtils.isResourceUnderLocation}。</p>
 */
class UploadStaticResourceConfigTest {

  /** 标准 1x1 PNG（真实字节，8 字节签名 + IHDR + IDAT + IEND）。 */
  private static final byte[] PNG_1X1 = Base64.getDecoder().decode(
      "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==");

  @TempDir
  Path tempDir;

  @Test
  @DisplayName("存储根目录内已存在的文件可被解析（修复前被字符串前缀口径误判为越界 → 500）")
  void guardResolvesExistingFileUnderRoot() throws IOException {
    Path root = newStorageRoot();
    Files.write(root.resolve("picture").resolve("pixel.png"), PNG_1X1);

    Resource resolved = new TestableGuard()
        .resolveForTest("picture/pixel.png", newLocationResource(root));

    assertThat(resolved).as("存在且位于根目录内的文件必须能被解析").isNotNull();
    assertThat(resolved.contentLength()).isEqualTo(PNG_1X1.length);
  }

  @Test
  @DisplayName("不存在的文件返回 null（由 ResourceHttpRequestHandler 抛 NoResourceFoundException → 404）")
  void guardReturnsNullForMissingFile() throws IOException {
    Path root = newStorageRoot();

    Resource resolved = new TestableGuard()
        .resolveForTest("picture/missing.png", newLocationResource(root));

    assertThat(resolved).isNull();
  }

  @Test
  @DisplayName("addResourceHandlers 为 /upload/** 注册 StorageRootGuardResolver")
  @SuppressWarnings("resource")
  void registersGuardResolverForUploadPattern() throws IOException {
    StorageProperties properties = new StorageProperties();
    properties.getLocal().setRoot(newStorageRoot());
    UploadStaticResourceConfig config = new UploadStaticResourceConfig(properties);

    StaticApplicationContext context = new StaticApplicationContext();
    try {
      TestableRegistry registry = new TestableRegistry(context);
      config.addResourceHandlers(registry);

      SimpleUrlHandlerMapping mapping = (SimpleUrlHandlerMapping) registry.exposedHandlerMapping();
      assertThat(mapping).isNotNull();
      // 脱离容器生命周期时，手动走 Aware 装配：setApplicationContext 会触发 registerHandlers 注册 handlerMap
      mapping.setApplicationContext(context);
      assertThat(mapping.getHandlerMap()).containsKey("/upload/**");

      ResourceHttpRequestHandler handler =
          (ResourceHttpRequestHandler) mapping.getHandlerMap().get("/upload/**");
      assertThat(handler.getResourceResolvers())
          .anySatisfy(resolver ->
              assertThat(resolver).isInstanceOf(UploadStaticResourceConfig.StorageRootGuardResolver.class));
    } finally {
      context.close();
    }
  }

  private Path newStorageRoot() throws IOException {
    Path root = tempDir.resolve("storage-root");
    Files.createDirectories(root.resolve("picture"));
    return root;
  }

  /** ResourceHandlerRegistry.getHandlerMapping 是 protected：测试用子类桥接暴露，不改生产代码可见性。 */
  static class TestableRegistry extends ResourceHandlerRegistry {

    TestableRegistry(ApplicationContext applicationContext) {
      super(applicationContext, null);
    }

    AbstractHandlerMapping exposedHandlerMapping() {
      return getHandlerMapping();
    }
  }

  /** PathResourceResolver.getResource 是 protected：测试用子类桥接调用。 */
  static class TestableGuard extends UploadStaticResourceConfig.StorageRootGuardResolver {

    Resource resolveForTest(String resourcePath, Resource location) throws IOException {
      return getResource(resourcePath, location);
    }
  }

  /** 与生产一致：location 字符串来自 Path.toUri().toString()（file:/// 形式），经 ResourceLoader 解析为 UrlResource。 */
  private Resource newLocationResource(Path root) {
    String location = root.toAbsolutePath().normalize().toUri().toString();
    return new DefaultResourceLoader().getResource(location.endsWith("/") ? location : location + "/");
  }
}

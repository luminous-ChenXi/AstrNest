package com.chenxi.astrnest.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  private static final String BEARER_AUTH = "bearerAuth";
  private static final String BASIC_AUTH = "basicAuth";

  @Bean
  public OpenAPI astrnestOpenAPI() {
    return new OpenAPI()
        .info(new Info()
            .title("AstrNest API")
            .version("v1")
            .description("现代化图床/媒体平台 API。基础路径 `/api/**`。鉴权以 Bearer JWT 为主通道："
                + "调用 `POST /api/auth/login` 获取 `token`（JWT，默认 30 天不活动过期，可用 `chenxi.passport.access-token-days` 配置），"
                + "后续请求携带 `Authorization: Bearer <JWT>`；过期后重新登录获取。"
                + "HTTP Basic（`Authorization: Basic <base64(user:password)>`）作为兼容通道保留，"
                + "便于 API 插件等非交互场景。管理员接口需额外权限。")
            .contact(new Contact().name("AstrNest Team")))
        .components(new Components()
            .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("主通道。`POST /api/auth/login` 返回的 JWT，格式 `Authorization: Bearer <JWT>`。"))
            .addSecuritySchemes(BASIC_AUTH, new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("basic")
                .description("兼容通道。`Authorization: Basic <base64(username:password)>`，行为与 JWT 通道一致。")))
        .security(List.of(
            new SecurityRequirement().addList(BEARER_AUTH),
            new SecurityRequirement().addList(BASIC_AUTH)))
        .externalDocs(new ExternalDocumentation()
            .description("项目 README 与部署说明")
            .url("https://github.com/luminous-ChenXi/astrnest"));
  }
}

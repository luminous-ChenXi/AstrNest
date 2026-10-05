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
  private static final String API_KEY_AUTH = "apiKeyAuth";

  @Bean
  public OpenAPI astrnestOpenAPI() {
    return new OpenAPI()
        .info(new Info()
            .title("AstrNest API")
            .version("v1")
            .description("现代化图床/媒体平台 API。基础路径 `/api/**`。鉴权以 Bearer JWT 为主通道："
                + "调用 `POST /api/auth/login` 获取 `token`（JWT，默认 30 天不活动过期，可用 `chenxi.passport.access-token-days` 配置），"
                + "后续请求携带 `Authorization: Bearer <JWT>`；过期后重新登录获取。"
                + "机器对机器场景（API 插件、脚本上传）使用 API Key：管理端签发，请求头 `X-API-Key: ik_...`。"
                + "管理员接口需额外权限。")
            .contact(new Contact().name("AstrNest Team")))
        .components(new Components()
            .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("主通道。`POST /api/auth/login` 返回的 JWT，格式 `Authorization: Bearer <JWT>`。"))
            .addSecuritySchemes(API_KEY_AUTH, new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.HEADER)
                .name("X-API-Key")
                .description("机器通道。管理端 `/api/keys` 签发的 API Key（`ik_` 前缀），仅可用于 `POST /api/uploads/**`。")))
        .security(List.of(
            new SecurityRequirement().addList(BEARER_AUTH),
            new SecurityRequirement().addList(API_KEY_AUTH)))
        .externalDocs(new ExternalDocumentation()
            .description("项目 README 与部署说明")
            .url("https://github.com/luminous-ChenXi/astrnest"));
  }
}

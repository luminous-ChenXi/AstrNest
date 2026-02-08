package com.chenxi.astrnest.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI astrnestOpenAPI() {
    return new OpenAPI()
        .info(new Info()
            .title("AstrNest API")
            .version("v1")
            .description("现代化图床/媒体平台 API。基础路径 `/api/**`，登录后将 Token 放入 `Authorization: Bearer <token>`，管理员接口需额外权限。")
            .contact(new Contact().name("AstrNest Team")))
        .externalDocs(new ExternalDocumentation()
            .description("项目 README 与部署说明")
            .url("https://github.com/luminous-ChenXi/astrnest"));
  }
}

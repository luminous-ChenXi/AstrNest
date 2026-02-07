package com.imgbed.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI imgbedOpenAPI() {
    return new OpenAPI()
        .info(new Info()
            .title("AstrNest API")
            .version("v1")
            .description("面向 Typora / Markdown 的现代化图床服务")
            .contact(new Contact().name("AstrNest Team")))
        .externalDocs(new ExternalDocumentation()
            .description("前端仓库")
            .url("https://example.com"));
  }
}

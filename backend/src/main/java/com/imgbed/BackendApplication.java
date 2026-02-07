package com.imgbed;

import com.imgbed.security.apikey.ApiKeyProperties;
import com.imgbed.security.config.AdminAccountProperties;
import com.imgbed.storage.StorageProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({StorageProperties.class, AdminAccountProperties.class, ApiKeyProperties.class})
public class BackendApplication {

  public static void main(String[] args) {
    SpringApplication.run(BackendApplication.class, args);
  }
}

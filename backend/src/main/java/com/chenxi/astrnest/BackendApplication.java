package com.chenxi.astrnest;

import com.chenxi.astrnest.config.CorsProperties;
import com.chenxi.astrnest.security.apikey.ApiKeyProperties;
import com.chenxi.astrnest.storage.StorageProperties;
import com.chenxi.astrnest.upload.media.VideoThumbnailProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({
    StorageProperties.class,
    ApiKeyProperties.class,
    VideoThumbnailProperties.class,
    CorsProperties.class
})
public class BackendApplication {

  public static void main(String[] args) {
    SpringApplication.run(BackendApplication.class, args);
  }
}

package com.imgbed.storage.s3;

import com.imgbed.storage.StorageProperties;
import com.imgbed.storage.StorageStrategy;
import org.springframework.stereotype.Component;

@Component
public class Ks3StorageHandler extends AbstractS3StorageHandler {

  public Ks3StorageHandler(StorageProperties properties) {
    super(properties);
  }

  @Override
  protected StorageProperties.S3Like config() {
    return properties().getKs3();
  }

  @Override
  protected StorageStrategy handlerStrategy() {
    return StorageStrategy.KS3;
  }
}

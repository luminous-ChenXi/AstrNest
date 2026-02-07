package com.imgbed.storage.s3;

import com.imgbed.storage.StorageProperties;
import com.imgbed.storage.StorageStrategy;
import org.springframework.stereotype.Component;

@Component
public class TencentCosStorageHandler extends AbstractS3StorageHandler {

  public TencentCosStorageHandler(StorageProperties properties) {
    super(properties);
  }

  @Override
  protected StorageProperties.S3Like config() {
    return properties().getCos();
  }

  @Override
  protected StorageStrategy handlerStrategy() {
    return StorageStrategy.TENCENT_COS;
  }
}

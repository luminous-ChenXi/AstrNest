package com.chenxi.astrnest.storage.s3;

import com.chenxi.astrnest.storage.StorageProperties;
import com.chenxi.astrnest.storage.StorageStrategy;
import org.springframework.stereotype.Component;

@Component
public class HuaweiObsStorageHandler extends AbstractS3StorageHandler {

  public HuaweiObsStorageHandler(StorageProperties properties) {
    super(properties);
  }

  @Override
  protected StorageProperties.S3Like config() {
    return properties().getObs();
  }

  @Override
  protected StorageStrategy handlerStrategy() {
    return StorageStrategy.HUAWEI_OBS;
  }
}

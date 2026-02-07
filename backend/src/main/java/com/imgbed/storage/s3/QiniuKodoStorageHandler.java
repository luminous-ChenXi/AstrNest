package com.imgbed.storage.s3;

import com.imgbed.storage.StorageProperties;
import com.imgbed.storage.StorageStrategy;
import org.springframework.stereotype.Component;

@Component
public class QiniuKodoStorageHandler extends AbstractS3StorageHandler {

  public QiniuKodoStorageHandler(StorageProperties properties) {
    super(properties);
  }

  @Override
  protected StorageProperties.S3Like config() {
    return properties().getKodo();
  }

  @Override
  protected StorageStrategy handlerStrategy() {
    return StorageStrategy.QINIU_KODO;
  }
}

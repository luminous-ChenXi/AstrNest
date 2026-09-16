package com.chenxi.astrnest.storage.handler;

import com.chenxi.astrnest.storage.StorageContext;
import com.chenxi.astrnest.storage.StorageStrategy;
import com.chenxi.astrnest.storage.StoredObject;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface StorageHandler {

  StorageStrategy strategy();

  StoredObject put(MultipartFile file, StorageContext context);

  void delete(String objectKey);

  Resource load(String objectKey);
}

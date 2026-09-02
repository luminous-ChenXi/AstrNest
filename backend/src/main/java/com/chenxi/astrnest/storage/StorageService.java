package com.chenxi.astrnest.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface StorageService {

  StoredObject store(MultipartFile file, StorageContext context);

  Resource loadAsResource(String objectKey);

  default Resource loadAsResource(String objectKey, String providerKey) {
    return loadAsResource(objectKey);
  }

  void delete(String objectKey, String providerKey);
}

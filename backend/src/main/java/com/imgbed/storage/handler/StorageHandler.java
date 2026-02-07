package com.imgbed.storage.handler;

import com.imgbed.storage.StorageContext;
import com.imgbed.storage.StorageStrategy;
import com.imgbed.storage.StoredObject;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface StorageHandler {

  StorageStrategy strategy();

  StoredObject put(MultipartFile file, StorageContext context);

  void delete(String objectKey);

  StorageListResult list(StorageListRequest request);

  String source(String objectKey);

  StorageTokenResponse token(StorageTokenRequest request);

  MediaMeta mediaMeta(String objectKey);

  Resource load(String objectKey);
}

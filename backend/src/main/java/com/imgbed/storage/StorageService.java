package com.imgbed.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface StorageService {

  StoredObject store(MultipartFile file, StorageContext context);

  Resource loadAsResource(String objectKey);
}

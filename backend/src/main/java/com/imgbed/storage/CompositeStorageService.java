package com.imgbed.storage;

import com.imgbed.storage.handler.StorageHandler;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class CompositeStorageService implements StorageService {

  private final List<StorageHandler> handlers;
  private final StorageProperties storageProperties;
  private Map<StorageStrategy, StorageHandler> handlerMap;

  private Map<StorageStrategy, StorageHandler> handlerMap() {
    if (handlerMap == null) {
      handlerMap = new EnumMap<>(StorageStrategy.class);
      for (StorageHandler handler : handlers) {
        handlerMap.put(handler.strategy(), handler);
      }
    }
    return handlerMap;
  }

  @Override
  public StoredObject store(MultipartFile file, StorageContext context) {
    StorageHandler handler = resolveHandler(context);
    StorageContext effectiveContext = context == null
        ? StorageContext.localPublicContext()
        : context;
    StoredObject stored = handler.put(file, effectiveContext);
    String providerKey = effectiveContext.providerKey() != null
        ? effectiveContext.providerKey()
        : handler.strategy().name();
    return new StoredObject(
        stored.objectKey(),
        stored.storedFileName(),
        stored.publicUrl(),
        stored.size(),
        stored.absolutePath(),
        providerKey
    );
  }

  @Override
  public Resource loadAsResource(String objectKey) {
    StorageHandler handler = resolveHandler(null);
    return handler.load(objectKey);
  }

  private StorageHandler resolveHandler(StorageContext context) {
    StorageStrategy strategy = context != null && context.strategy() != null
        ? context.strategy()
        : storageProperties.getStrategy();
    StorageHandler handler = handlerMap().get(strategy);
    if (handler == null) {
      throw new IllegalStateException("未找到存储策略: " + strategy);
    }
    return handler;
  }
}

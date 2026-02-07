package com.chenxi.astrnest.storage;

public class StorageObjectNotFoundException extends RuntimeException {
  public StorageObjectNotFoundException(String objectKey) {
    super("Object not found: " + objectKey);
  }
}

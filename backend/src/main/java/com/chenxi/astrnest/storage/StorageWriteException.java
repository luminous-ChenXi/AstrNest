package com.chenxi.astrnest.storage;

public class StorageWriteException extends RuntimeException {
  public StorageWriteException(String message, Throwable cause) {
    super(message, cause);
  }

  public StorageWriteException(String message) {
    super(message);
  }
}

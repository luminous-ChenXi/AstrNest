package com.imgbed.security.apikey.exception;

public class ApiKeyQuotaExceededException extends RuntimeException {

  public ApiKeyQuotaExceededException(String message) {
    super(message);
  }
}

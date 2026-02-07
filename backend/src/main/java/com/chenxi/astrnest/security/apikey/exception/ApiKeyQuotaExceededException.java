package com.chenxi.astrnest.security.apikey.exception;

public class ApiKeyQuotaExceededException extends RuntimeException {

  public ApiKeyQuotaExceededException(String message) {
    super(message);
  }
}

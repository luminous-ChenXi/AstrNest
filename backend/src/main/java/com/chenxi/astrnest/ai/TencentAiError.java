package com.chenxi.astrnest.ai;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TencentAiError(
    String code,
    String message,
    String friendlyMessage,
    String requestId,
    Integer statusCode
) {

  public static TencentAiError of(String code, String message, String friendlyMessage,
      String requestId, Integer statusCode) {
    return new TencentAiError(code, message, friendlyMessage, requestId, statusCode);
  }
}

package com.chenxi.astrnest.common;

import com.chenxi.astrnest.security.apikey.exception.ApiKeyAuthenticationException;
import com.chenxi.astrnest.security.apikey.exception.ApiKeyQuotaExceededException;
import com.chenxi.astrnest.storage.StorageObjectNotFoundException;
import com.chenxi.astrnest.storage.StorageWriteException;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
    return buildResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(StorageWriteException.class)
  public ResponseEntity<ApiErrorResponse> handleStorageWrite(StorageWriteException ex) {
    return buildResponse("文件保存失败，请稍后重试", HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @ExceptionHandler(StorageObjectNotFoundException.class)
  public ResponseEntity<ApiErrorResponse> handleNotFound(StorageObjectNotFoundException ex) {
    return buildResponse("文件不存在", HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public ResponseEntity<ApiErrorResponse> handleMaxSize(MaxUploadSizeExceededException ex) {
    return buildResponse("文件过大", HttpStatus.PAYLOAD_TOO_LARGE);
  }

  @ExceptionHandler(ApiKeyAuthenticationException.class)
  public ResponseEntity<ApiErrorResponse> handleApiKeyAuth(ApiKeyAuthenticationException ex) {
    return buildResponse(ex.getMessage(), HttpStatus.UNAUTHORIZED);
  }

  @ExceptionHandler(ApiKeyQuotaExceededException.class)
  public ResponseEntity<ApiErrorResponse> handleQuota(ApiKeyQuotaExceededException ex) {
    return buildResponse(ex.getMessage(), HttpStatus.TOO_MANY_REQUESTS);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
    String message = ex.getBindingResult().getFieldErrors().stream()
        .findFirst()
        .map(error -> error.getField() + " " + error.getDefaultMessage())
        .orElse("请求参数不合法");
    return buildResponse(message, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<ApiErrorResponse> handleResponseStatus(ResponseStatusException ex) {
    HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
    if (status == null) {
      status = HttpStatus.INTERNAL_SERVER_ERROR;
    }
    return buildResponse(ex.getReason(), status);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiErrorResponse> handleOther(Exception ex) {
    ex.printStackTrace();
    return buildResponse("服务器内部错误: " + ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
  }

  private ResponseEntity<ApiErrorResponse> buildResponse(String message, HttpStatus status) {
    ApiErrorResponse body = ApiErrorResponse.of(
        Objects.requireNonNullElse(message, "服务器错误"),
        status.value()
    );
    return ResponseEntity.status(status)
        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
        .body(body);
  }
}

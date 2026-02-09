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
    String userMessage = translateS3Error(ex);
    return buildResponse(userMessage, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  /**
   * 将 S3/AWS 技术错误转换为用户友好的中文提示
   */
  private String translateS3Error(StorageWriteException ex) {
    Throwable cause = ex.getCause();
    String errorMessage = cause != null ? cause.getMessage() : ex.getMessage();
    
    if (errorMessage == null) {
      return "文件上传失败，请稍后重试";
    }
    
    String lowerMsg = errorMessage.toLowerCase();
    
    // DNS 解析错误 - 端点无法访问
    if (lowerMsg.contains("unknownhost") || lowerMsg.contains("dns") || lowerMsg.contains("无法解析")) {
      return "存储服务连接失败：无法连接到存储服务器，请检查存储配置中的区域(Region)和端点(Endpoint)是否正确";
    }
    
    // 加速模式与路径样式冲突
    if (lowerMsg.contains("accelerate") && lowerMsg.contains("path style")) {
      return "存储配置错误：传输加速模式不能与路径样式寻址同时使用。请在存储策略设置中关闭其中一个选项（建议关闭加速模式）";
    }
    
    // 访问密钥错误
    if (lowerMsg.contains("access key") || lowerMsg.contains("secret key") || 
        lowerMsg.contains("signature") || lowerMsg.contains("credentials") ||
        lowerMsg.contains("ak") || lowerMsg.contains("sk")) {
      return "存储认证失败：访问密钥(Access Key)或密钥(Secret Key)不正确，请检查存储策略中的密钥配置";
    }
    
    // Bucket 不存在或无权访问
    if (lowerMsg.contains("bucket") && (lowerMsg.contains("not exist") || 
        lowerMsg.contains("no such bucket") || lowerMsg.contains("access denied") ||
        lowerMsg.contains("forbidden"))) {
      return "存储桶访问失败：存储桶(Bucket)不存在或没有访问权限，请检查存储桶名称和权限配置";
    }
    
    // 网络连接超时
    if (lowerMsg.contains("timeout") || lowerMsg.contains("connect") || lowerMsg.contains("connection")) {
      return "网络连接超时：无法连接到存储服务，请检查网络连接或稍后重试";
    }
    
    // 存储空间已满
    if (lowerMsg.contains("quota") || lowerMsg.contains("space") || lowerMsg.contains("full") ||
        lowerMsg.contains("insufficient")) {
      return "存储空间不足：存储桶空间已满，请联系管理员扩容或清理存储空间";
    }
    
    // 文件过大
    if (lowerMsg.contains("size") || lowerMsg.contains("large") || lowerMsg.contains("big")) {
      return "文件过大：超出存储服务允许的文件大小限制";
    }
    
    // 默认错误
    return "文件上传失败：" + ex.getMessage();
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

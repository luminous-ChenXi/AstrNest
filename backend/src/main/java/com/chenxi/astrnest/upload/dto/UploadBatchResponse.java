package com.chenxi.astrnest.upload.dto;

import java.util.List;

/**
 * 批次上传响应 DTO
 * 包含成功上传的文件、被跳过的文件以及人性化提示消息
 */
public class UploadBatchResponse {

  private List<UploadResponse> uploaded;
  private List<SkippedFileInfo> skipped;
  private String message;

  public UploadBatchResponse() {
  }

  public UploadBatchResponse(List<UploadResponse> uploaded, List<SkippedFileInfo> skipped,
      String message) {
    this.uploaded = uploaded;
    this.skipped = skipped;
    this.message = message;
  }

  public List<UploadResponse> getUploaded() {
    return uploaded;
  }

  public void setUploaded(List<UploadResponse> uploaded) {
    this.uploaded = uploaded;
  }

  public List<SkippedFileInfo> getSkipped() {
    return skipped;
  }

  public void setSkipped(List<SkippedFileInfo> skipped) {
    this.skipped = skipped;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  /**
   * 被跳过的文件信息
   */
  public static class SkippedFileInfo {

    private String fileName;
    private String reason;

    public SkippedFileInfo() {
    }

    public SkippedFileInfo(String fileName, String reason) {
      this.fileName = fileName;
      this.reason = reason;
    }

    public String getFileName() {
      return fileName;
    }

    public void setFileName(String fileName) {
      this.fileName = fileName;
    }

    public String getReason() {
      return reason;
    }

    public void setReason(String reason) {
      this.reason = reason;
    }
  }
}

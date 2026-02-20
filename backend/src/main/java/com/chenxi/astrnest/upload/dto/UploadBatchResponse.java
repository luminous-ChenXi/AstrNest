package com.chenxi.astrnest.upload.dto;

import java.util.List;

public record UploadBatchResponse(
    List<UploadResponse> uploaded,
    List<SkippedFileInfo> skipped,
    String message
) {
  public UploadBatchResponse {
    uploaded = uploaded != null ? uploaded : List.of();
    skipped = skipped != null ? skipped : List.of();
  }

  public record SkippedFileInfo(
      String fileName,
      String reason
  ) {}
}

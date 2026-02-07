package com.imgbed.admin.upload.dto;

import java.util.List;

public record AdminUploadPageResponse(
    List<AdminUploadItemResponse> records,
    long totalElements,
    int totalPages,
    int page,
    int size
) {
}

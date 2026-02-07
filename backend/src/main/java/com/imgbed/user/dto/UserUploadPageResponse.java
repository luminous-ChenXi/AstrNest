package com.imgbed.user.dto;

import java.util.List;

public record UserUploadPageResponse(
    List<UserUploadItemResponse> items,
    long total,
    int page,
    int size
) {}

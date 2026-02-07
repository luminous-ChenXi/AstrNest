package com.imgbed.gallery.dto;

import java.util.List;

public record PublicGalleryPageResponse(
    List<PublicGalleryItemResponse> items,
    long totalElements,
    int totalPages,
    int page,
    int size,
    boolean guestLikeEnabled
) {}

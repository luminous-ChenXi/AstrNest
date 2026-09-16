package com.chenxi.astrnest.announcement.dto;

import java.util.List;

public record AnnouncementPageResponse(
    List<AnnouncementResponse> items,
    long totalElements,
    int totalPages,
    int page,
    int size
) {}

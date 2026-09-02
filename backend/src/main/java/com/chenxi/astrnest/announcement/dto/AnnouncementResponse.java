package com.chenxi.astrnest.announcement.dto;

import com.chenxi.astrnest.announcement.AnnouncementLevel;
import com.chenxi.astrnest.announcement.AnnouncementStatus;
import java.time.Instant;

public record AnnouncementResponse(
    Long id,
    String title,
    String summary,
    AnnouncementLevel level,
    AnnouncementStatus status,
    boolean pinned,
    Instant publishedAt,
    Instant updatedAt,
    String author,
    Long authorUserId,
    String authorRole,
    String authorAvatar,
    String contentMarkdown
) {}

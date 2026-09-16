package com.chenxi.astrnest.announcement.dto;

import com.chenxi.astrnest.announcement.AnnouncementLevel;
import com.chenxi.astrnest.announcement.AnnouncementStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record AnnouncementRequest(
    @NotBlank @Size(max = 180) String title,
    @Size(max = 360) String summary,
    AnnouncementLevel level,
    AnnouncementStatus status,
    boolean pinned,
    @NotBlank String contentMarkdown,
    Instant publishedAt,
    String author
) {}

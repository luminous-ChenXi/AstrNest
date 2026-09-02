package com.chenxi.astrnest.chenxi.mail.dto;

import java.time.Instant;
import java.util.List;

public record ChenxiMailTemplateResponse(
    Long id,
    String name,
    String type,
    String subject,
    String content,
    List<String> variables,
    Instant createdAt,
    Instant updatedAt
) {
}

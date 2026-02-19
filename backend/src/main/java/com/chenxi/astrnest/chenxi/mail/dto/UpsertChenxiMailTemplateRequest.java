package com.chenxi.astrnest.chenxi.mail.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record UpsertChenxiMailTemplateRequest(
    @NotBlank @Size(max = 120) String name,
    @NotBlank @Size(max = 60) String type,
    @NotBlank @Size(max = 200) String subject,
    @NotBlank String content,
    List<@Size(max = 60) String> variables
) {
}

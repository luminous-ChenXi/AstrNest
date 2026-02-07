package com.imgbed.chenxi.mail.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;

public record TestMailTemplateRequest(
    @NotBlank @Email String targetEmail,
    Map<String, String> params
) {
}

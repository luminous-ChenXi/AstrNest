package com.chenxi.astrnest.chenxi.mail.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateChenxiMailConfigRequest(
    @NotBlank String smtpHost,
    @NotNull Integer smtpPort,
    @NotBlank String smtpUsername,
    @Size(max = 200) String smtpPassword,
    @NotBlank @Size(max = 20) String secureType,
    @NotBlank @Email String fromEmail,
    @NotBlank @Size(max = 120) String fromName,
    boolean enabled
) {
}

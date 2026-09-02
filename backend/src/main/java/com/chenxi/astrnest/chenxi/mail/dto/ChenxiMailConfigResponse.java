package com.chenxi.astrnest.chenxi.mail.dto;

import com.chenxi.astrnest.chenxi.mail.ChenxiMailConfig;

public record ChenxiMailConfigResponse(
    String smtpHost,
    Integer smtpPort,
    String smtpUsername,
    String secureType,
    String fromEmail,
    String fromName,
    boolean enabled
) {
  public static ChenxiMailConfigResponse fromEntity(ChenxiMailConfig config) {
    return new ChenxiMailConfigResponse(
        config.getSmtpHost(),
        config.getSmtpPort(),
        config.getSmtpUsername(),
        config.getSecureType(),
        config.getFromEmail(),
        config.getFromName(),
        config.isEnabled()
    );
  }
}

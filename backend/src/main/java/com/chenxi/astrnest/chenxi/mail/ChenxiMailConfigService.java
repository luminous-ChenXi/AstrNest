package com.chenxi.astrnest.chenxi.mail;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ChenxiMailConfigService {

  private final ChenxiMailConfigRepository repository;

  public ChenxiMailConfig getOrDefault() {
    return repository.findById(1L).orElseGet(this::buildDefaultConfig);
  }

  @Transactional
  public ChenxiMailConfig save(ChenxiMailConfig payload, String operator) {
    ChenxiMailConfig config = repository.findById(1L).orElse(new ChenxiMailConfig());
    config.setSmtpHost(payload.getSmtpHost());
    config.setSmtpPort(payload.getSmtpPort());
    config.setSmtpUsername(payload.getSmtpUsername());
    if (StringUtils.hasText(payload.getSmtpPassword())) {
      config.setSmtpPassword(payload.getSmtpPassword());
    } else if (config.getSmtpPassword() == null) {
      throw new IllegalArgumentException("请填写邮箱授权码");
    }
    String secureType = StringUtils.hasText(payload.getSecureType()) ? payload.getSecureType() : "ssl";
    config.setSecureType(secureType);
    config.setFromEmail(payload.getFromEmail());
    config.setFromName(payload.getFromName());
    config.setEnabled(payload.isEnabled());
    config.setUpdatedBy(operator);
    return repository.save(config);
  }

  private ChenxiMailConfig buildDefaultConfig() {
    ChenxiMailConfig config = new ChenxiMailConfig();
    config.setSmtpHost("smtp.example.com");
    config.setSmtpPort(465);
    config.setSmtpUsername("no-reply@example.com");
    config.setSmtpPassword("CHANGE_ME");
    config.setSecureType("ssl");
    config.setFromEmail("no-reply@example.com");
    config.setFromName("AstrNest Mailer");
    config.setEnabled(false);
    config.setUpdatedBy("system");
    return config;
  }
}

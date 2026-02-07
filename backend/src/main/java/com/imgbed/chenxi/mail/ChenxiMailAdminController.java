package com.imgbed.chenxi.mail;

import com.imgbed.chenxi.mail.dto.ChenxiMailConfigResponse;
import com.imgbed.chenxi.mail.dto.TestMailRequest;
import com.imgbed.chenxi.mail.dto.UpdateChenxiMailConfigRequest;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/chenxi/mail-config")
@RequiredArgsConstructor
public class ChenxiMailAdminController {

  private final ChenxiMailConfigService configService;
  private final ChenxiMailService mailService;

  @GetMapping
  public ChenxiMailConfigResponse getConfig() {
    return ChenxiMailConfigResponse.fromEntity(configService.getOrDefault());
  }

  @PutMapping
  public ChenxiMailConfigResponse updateConfig(
      @Valid @RequestBody UpdateChenxiMailConfigRequest request,
      Authentication authentication
  ) {
    ChenxiMailConfig payload = new ChenxiMailConfig();
    payload.setSmtpHost(request.smtpHost());
    payload.setSmtpPort(request.smtpPort());
    payload.setSmtpUsername(request.smtpUsername());
    payload.setSmtpPassword(request.smtpPassword());
    payload.setSecureType(request.secureType());
    payload.setFromEmail(request.fromEmail());
    payload.setFromName(request.fromName());
    payload.setEnabled(request.enabled());
    String operator = authentication != null ? authentication.getName() : "system";
    ChenxiMailConfig saved = configService.save(payload, operator);
    return ChenxiMailConfigResponse.fromEntity(saved);
  }

  @PostMapping("/test")
  public Map<String, String> testConnection(@Valid @RequestBody TestMailRequest request) {
    mailService.sendTestMail(request.targetEmail());
    return Map.of("message", "测试邮件已发送，请查收");
  }
}

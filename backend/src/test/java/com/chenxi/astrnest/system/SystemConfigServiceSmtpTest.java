package com.chenxi.astrnest.system;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.chenxi.astrnest.chenxi.mail.ChenxiMailConfig;
import com.chenxi.astrnest.chenxi.mail.ChenxiMailConfigService;
import com.chenxi.astrnest.security.user.UserAccountRepository;
import com.chenxi.astrnest.upload.record.UploadRecordRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

/**
 * {@link SystemConfigService#isSmtpConfigured()} 判定口径回归测试。
 *
 * <p>历史缺陷：强制要求 smtpPassword 非空，无鉴权 SMTP（MailPit/内网中继）只填
 * host+from 时被误报「未配置」。修复后：host+from 非空即视为已配置，密码仅在
 * 同时配置了 smtpUsername 时才必填，CHANGE_ME 占位符不视为真实密码（与 LuomiBlog 口径一致）。</p>
 */
@ExtendWith(MockitoExtension.class)
class SystemConfigServiceSmtpTest {

  @Mock
  private SystemConfigRepository systemConfigRepository;

  @Mock
  private UserAccountRepository userAccountRepository;

  @Mock
  private UploadRecordRepository uploadRecordRepository;

  @Mock
  private Environment environment;

  @Mock
  private ChenxiMailConfigService chenxiMailConfigService;

  @InjectMocks
  private SystemConfigService systemConfigService;

  private void givenMailConfig(String host, String username, String password, String from, boolean enabled) {
    ChenxiMailConfig config = new ChenxiMailConfig();
    config.setSmtpHost(host);
    config.setSmtpUsername(username);
    config.setSmtpPassword(password);
    config.setFromEmail(from);
    config.setEnabled(enabled);
    when(chenxiMailConfigService.getOrDefault()).thenReturn(config);
  }

  @Test
  @DisplayName("只填 host+from（无鉴权 SMTP，用户名密码留空）即视为已配置——修复前误报未配置")
  void hostAndFromOnlyCountsAsConfigured() {
    givenMailConfig("smtp.mailpit.internal", "", "", "noreply@example.com", true);

    assertThat(systemConfigService.isSmtpConfigured()).isTrue();
  }

  @Test
  @DisplayName("配置了用户名时密码必填：有用户名无密码视为未配置")
  void usernameWithoutPasswordCountsAsNotConfigured() {
    givenMailConfig("smtp.qq.com", "noreply@example.com", "", "noreply@example.com", true);

    assertThat(systemConfigService.isSmtpConfigured()).isFalse();
  }

  @Test
  @DisplayName("配置了用户名但密码是 CHANGE_ME 占位符视为未配置")
  void changeMePasswordWithUsernameCountsAsNotConfigured() {
    givenMailConfig("smtp.qq.com", "noreply@example.com", "CHANGE_ME", "noreply@example.com", true);

    assertThat(systemConfigService.isSmtpConfigured()).isFalse();
  }

  @Test
  @DisplayName("用户名 + 真实密码视为已配置")
  void usernameWithRealPasswordCountsAsConfigured() {
    givenMailConfig("smtp.qq.com", "noreply@example.com", "real-secret", "noreply@example.com", true);

    assertThat(systemConfigService.isSmtpConfigured()).isTrue();
  }

  @Test
  @DisplayName("未启用、示例占位 host、from 缺失均视为未配置")
  void disabledOrPlaceholderCountsAsNotConfigured() {
    givenMailConfig("smtp.example.com", "no-reply@example.com", "CHANGE_ME", "no-reply@example.com", false);
    assertThat(systemConfigService.isSmtpConfigured()).isFalse();

    when(chenxiMailConfigService.getOrDefault())
        .thenReturn(mailConfigWith("smtp.example.com", true));
    assertThat(systemConfigService.isSmtpConfigured()).isFalse();

    when(chenxiMailConfigService.getOrDefault())
        .thenReturn(mailConfigWith("smtp.qq.com", false));
    assertThat(systemConfigService.isSmtpConfigured()).isFalse();
  }

  private ChenxiMailConfig mailConfigWith(String host, boolean hasFrom) {
    ChenxiMailConfig config = new ChenxiMailConfig();
    config.setSmtpHost(host);
    config.setSmtpUsername("someone");
    config.setSmtpPassword("real-secret");
    config.setFromEmail(hasFrom ? "noreply@example.com" : "");
    config.setEnabled(true);
    return config;
  }
}

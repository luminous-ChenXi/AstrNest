package com.chenxi.astrnest.chenxi.mail;

import com.chenxi.astrnest.chenxi.auth.ChenxiEmailScene;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ChenxiMailService {

  private static final DateTimeFormatter TIMESTAMP_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.CHINA)
          .withZone(ZoneId.systemDefault());

  private final ChenxiMailConfigService mailConfigService;

  public void sendVerificationMail(String targetEmail, String code, ChenxiEmailScene scene) {
    String recipient = Objects.requireNonNull(targetEmail, "目标邮箱不能为空");
    String verificationCode = Objects.requireNonNull(code, "验证码不能为空");
    ChenxiEmailScene mailScene = Objects.requireNonNull(scene, "邮件场景不能为空");
    ChenxiMailConfig config = ensureEnabledConfig();
    SenderIdentity senderIdentity = senderIdentity(config);
    JavaMailSenderImpl sender = buildSender(config);
    try {
      MimeMessage message = sender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, StandardCharsets.UTF_8.name());
      helper.setTo(recipient);
      helper.setFrom(
          Objects.requireNonNull(senderIdentity.email(), "发件邮箱不能为空"),
          Objects.requireNonNull(senderIdentity.name(), "发件人名称不能为空")
      );
      String subject = prefix(mailScene) + "验证码：" + verificationCode;
      String body = buildHtmlBody(senderIdentity.name(), verificationCode, mailScene);
      helper.setSubject(subject);
      helper.setText(Objects.requireNonNull(body), true);
      sender.send(message);
    } catch (MessagingException | UnsupportedEncodingException | MailException ex) {
      throw new IllegalStateException("邮件发送失败，请检查 SMTP 配置", ex);
    }
  }

  public void sendTestMail(String targetEmail) {
    String recipient = Objects.requireNonNull(targetEmail, "目标邮箱不能为空");
    ChenxiMailConfig config = ensureEnabledConfig();
    SenderIdentity senderIdentity = senderIdentity(config);
    JavaMailSenderImpl sender = buildSender(config);
    try {
      MimeMessage message = sender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, StandardCharsets.UTF_8.name());
      helper.setTo(recipient);
      helper.setFrom(
          Objects.requireNonNull(senderIdentity.email(), "发件邮箱不能为空"),
          Objects.requireNonNull(senderIdentity.name(), "发件人名称不能为空")
      );
      helper.setSubject("【辰汐】SMTP 测试邮件");
      String body = "<p>这是一封来自辰汐图床的 SMTP 测试邮件。</p><p>若您收到此邮件，代表邮件服务配置成功 ✅</p><p>感谢您选择辰汐图床！</p><p>触发时间："
          + TIMESTAMP_FORMATTER.format(Instant.now()) + "</p>";
      helper.setText(body, true);
      sender.send(message);
    } catch (MessagingException | UnsupportedEncodingException | MailException ex) {
      throw new IllegalStateException("测试邮件发送失败，请检查配置", ex);
    }
  }

  public void sendTemplateMail(String targetEmail, ChenxiMailTemplate template, Map<String, String> params) {
    String recipient = Objects.requireNonNull(targetEmail, "目标邮箱不能为空");
    ChenxiMailTemplate mailTemplate = Objects.requireNonNull(template, "邮件模板不能为空");
    Map<String, String> payload = params == null ? Map.of() : params;
    ChenxiMailConfig config = ensureEnabledConfig();
    SenderIdentity senderIdentity = senderIdentity(config);
    JavaMailSenderImpl sender = buildSender(config);
    try {
      MimeMessage message = sender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, StandardCharsets.UTF_8.name());
      helper.setTo(recipient);
      helper.setFrom(
          Objects.requireNonNull(senderIdentity.email(), "发件邮箱不能为空"),
          Objects.requireNonNull(senderIdentity.name(), "发件人名称不能为空")
      );
      String subject = Objects.requireNonNull(applyParams(mailTemplate.getSubject(), payload), "邮件主题不能为空");
      String body = Objects.requireNonNull(applyParams(mailTemplate.getContent(), payload), "邮件正文不能为空");
      helper.setSubject(subject);
      helper.setText(body, true);
      sender.send(message);
    } catch (MessagingException | UnsupportedEncodingException | MailException ex) {
      throw new IllegalStateException("模板邮件发送失败，请检查配置", ex);
    }
  }

  private ChenxiMailConfig ensureEnabledConfig() {
    ChenxiMailConfig config = mailConfigService.getOrDefault();
    if (!config.isEnabled() || !StringUtils.hasText(config.getSmtpPassword())) {
      throw new IllegalStateException("请先在后台启用并完善 SMTP 配置");
    }
    if (!StringUtils.hasText(config.getFromEmail()) || !StringUtils.hasText(config.getFromName())) {
      throw new IllegalStateException("请配置发件邮箱与发件人名称");
    }
    return config;
  }

  private JavaMailSenderImpl buildSender(ChenxiMailConfig config) {
    JavaMailSenderImpl sender = new JavaMailSenderImpl();
    sender.setHost(config.getSmtpHost());
    sender.setPort(config.getSmtpPort());
    sender.setUsername(config.getSmtpUsername());
    sender.setPassword(config.getSmtpPassword());
    sender.setDefaultEncoding(StandardCharsets.UTF_8.name());
    Properties props = sender.getJavaMailProperties();
    props.put("mail.smtp.auth", "true");
    props.put("mail.smtp.timeout", "5000");
    props.put("mail.smtp.connectiontimeout", "5000");
    String secure = config.getSecureType();
    if ("ssl".equalsIgnoreCase(secure)) {
      props.put("mail.smtp.ssl.enable", "true");
    } else if ("tls".equalsIgnoreCase(secure)) {
      props.put("mail.smtp.starttls.enable", "true");
    }
    return sender;
  }

  private SenderIdentity senderIdentity(ChenxiMailConfig config) {
    String fromEmail = Objects.requireNonNull(config.getFromEmail(), "发件邮箱未配置");
    String fromName = Objects.requireNonNull(config.getFromName(), "发件人名称未配置");
    return new SenderIdentity(fromEmail, fromName);
  }

  private record SenderIdentity(String email, String name) {}

  private String applyParams(String source, Map<String, String> params) {
    if (!StringUtils.hasText(source)) {
      return "";
    }
    if (params == null || params.isEmpty()) {
      return source;
    }
    String result = source;
    for (Map.Entry<String, String> entry : params.entrySet()) {
      String placeholder = "{{" + entry.getKey() + "}}";
      String value = entry.getValue() == null ? "" : entry.getValue();
      result = result.replace(placeholder, value);
    }
    return result;
  }

  private String prefix(ChenxiEmailScene scene) {
    return switch (scene) {
      case REGISTER -> "【辰汐注册】";
      case PASSWORD_RESET -> "【辰汐找回】";
    };
  }

  private String buildHtmlBody(String senderName, String code, ChenxiEmailScene scene) {
    return "<div style=\"font-family:Inter,\u601D\u6E90 Hei,Helvetica,sans-serif;max-width:520px;margin:0 auto;padding:24px;background:#0f172a;color:#f8fafc;border-radius:20px;\">"
        + "<h2 style=\"margin-top:0;color:#fdbb2d;letter-spacing:2px;\">CHENXI AUTH" + "</h2>"
        + "<p style=\"font-size:15px;line-height:1.7;color:#e2e8f0;\">"
        + (scene == ChenxiEmailScene.REGISTER ? "欢迎注册辰汐内容治理平台，验证码如下：" : "您正在重置辰汐账户密码，验证码如下：")
        + "</p>"
        + "<div style=\"margin:24px 0;padding:18px 24px;border-radius:18px;background:rgba(99,102,241,0.1);border:1px solid rgba(99,102,241,0.4);\">"
        + "<div style=\"font-size:32px;font-weight:600;letter-spacing:6px;color:#f8fafc;text-align:center;\">"
        + code
        + "</div>"
        + "<p style=\"margin:12px 0 0;text-align:center;font-size:13px;color:#94a3b8;\">验证码 5 分钟内有效，请勿泄露给他人</p>"
        + "</div>"
        + "<p style=\"font-size:13px;color:#94a3b8;\">如果这不是您本人操作，请忽略本邮件；重复触发可能导致账号被保护性锁定。</p>"
        + "<p style=\"font-size:12px;color:#64748b;margin-top:32px;\">辰汐安全平台 · This email was sent by "
        + "<strong>" + senderName + "</strong></p>"
        + "</div>";
  }
}

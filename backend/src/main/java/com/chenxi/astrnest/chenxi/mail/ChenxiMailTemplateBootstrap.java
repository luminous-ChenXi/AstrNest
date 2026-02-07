package com.chenxi.astrnest.chenxi.mail;

import com.chenxi.astrnest.chenxi.mail.dto.UpsertChenxiMailTemplateRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChenxiMailTemplateBootstrap implements ApplicationRunner {

  private static final String SYSTEM_OPERATOR = "system";
  private static final String DEFAULT_TEMPLATE_TYPE = "register";
  private static final String DEFAULT_TEMPLATE_NAME = "注册验证模板";
  private static final String DEFAULT_TEMPLATE_SUBJECT = "【辰汐图床】邮箱验证码：{{code}}";
  private static final List<String> DEFAULT_TEMPLATE_VARIABLES = List.of("username", "code", "expireMinutes");
  private static final String DEFAULT_TEMPLATE_CONTENT = """
      <!DOCTYPE html>
      <html lang=\"zh-CN\">
      <head>
        <meta charset=\"UTF-8\">
        <title>辰汐图床验证码</title>
      </head>
      <body style=\"background-color:#0f172a;padding:32px;font-family:'Segoe UI',Arial,sans-serif;color:#e2e8f0;\">
        <div style=\"max-width:520px;margin:0 auto;background:rgba(15,23,42,0.85);padding:32px;border-radius:16px;border:1px solid rgba(148,163,184,0.2);\">
          <h2 style=\"color:#a78bfa;margin-bottom:12px;\">{{username}}，欢迎加入辰汐图床</h2>
          <p style=\"margin:0 0 16px;line-height:1.7;\">请使用以下验证码完成操作（注册 / 重置 / 验证）。</p>
          <div style=\"text-align:center;margin:32px 0;padding:24px;background:rgba(99,102,241,0.12);border-radius:12px;border:1px solid rgba(129,140,248,0.35);\">
            <p style=\"margin:0;color:#94a3b8;font-size:14px;letter-spacing:0.4em;text-transform:uppercase;\">验证码</p>
            <p style=\"margin:8px 0 0;font-size:32px;font-weight:600;color:#f8fafc;letter-spacing:0.3em;\">{{code}}</p>
          </div>
          <p style=\"margin:0 0 8px;line-height:1.7;\">该验证码在 <strong>{{expireMinutes}}</strong> 分钟内有效，请勿泄露给他人。</p>
          <p style=\"margin:0 0 16px;line-height:1.7;color:#94a3b8;\">如果这不是你的操作，请忽略本邮件。</p>
          <p style=\"margin:24px 0 0;color:#94a3b8;font-size:13px;\">— 辰汐图床 · 自动通知</p>
        </div>
      </body>
      </html>
      """;

  private final ChenxiMailTemplateRepository repository;
  private final ChenxiMailTemplateService templateService;

  @Override
  public void run(ApplicationArguments args) {
    if (repository.existsByType(DEFAULT_TEMPLATE_TYPE)) {
      return;
    }
    UpsertChenxiMailTemplateRequest request = new UpsertChenxiMailTemplateRequest(
        DEFAULT_TEMPLATE_NAME,
        DEFAULT_TEMPLATE_TYPE,
        DEFAULT_TEMPLATE_SUBJECT,
        DEFAULT_TEMPLATE_CONTENT,
        DEFAULT_TEMPLATE_VARIABLES
    );
    templateService.create(request, SYSTEM_OPERATOR);
  }
}

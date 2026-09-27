package com.chenxi.astrnest.chenxi.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "chenxi_email_token", indexes = {
    @Index(name = "idx_chenxi_email_scene", columnList = "email,scene")
})
@Getter
@Setter
public class ChenxiEmailToken {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 180)
  private String email;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  private ChenxiEmailScene scene;

  @Column(nullable = false, length = 6)
  private String code;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "consumed_at")
  private Instant consumedAt;

  @Column(nullable = false)
  private boolean consumed = false;

  @Column(nullable = false)
  private int attempts = 0;

  @Column(name = "resend_available_at", nullable = false)
  private Instant resendAvailableAt;

  @Column(name = "captcha_token", length = 64)
  private String captchaToken;

  /**
   * 注册验证链接令牌（仅 REGISTER 场景生成）：邮件同时携带 6 位验证码与本令牌的落地链接，
   * 用户可「输码」或「点链接」完成验证；30 分钟有效，与 code 同生共死（消费即焚）。
   */
  @Column(name = "link_token", length = 64)
  private String linkToken;
}

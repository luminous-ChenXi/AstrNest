package com.chenxi.astrnest.chenxi.captcha;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "chenxi_captcha_ticket", indexes = {
    @Index(name = "idx_chenxi_captcha_token", columnList = "verification_token")
})
@Getter
@Setter
public class ChenxiCaptchaTicket {

  @Id
  @Column(length = 64)
  private String id;

  @Column(name = "expected_offset", nullable = false)
  private double expectedOffset;

  @Column(nullable = false)
  private double tolerance;

  @Column(name = "captcha_code", length = 16)
  private String captchaCode;

  @Column(nullable = false)
  private int attempts = 0;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  @Column(nullable = false)
  private boolean verified = false;

  @Column(name = "verification_token", length = 64)
  private String verificationToken;

  @Column(name = "verification_token_expires", nullable = false)
  private Instant verificationTokenExpires;

  @Column(name = "cert_consumed", nullable = false)
  private boolean certificationConsumed = false;

  @Column(name = "verified_at")
  private Instant verifiedAt;
}

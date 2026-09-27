package com.chenxi.astrnest.system;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "system_config")
@Getter
@Setter
public class SystemConfig {

  @Id
  private Long id = 1L;

  @Column(nullable = false)
  private long maxUploadBytes = 20L * 1024 * 1024;

  @Column(name = "max_video_upload_bytes", nullable = false)
  private long maxVideoUploadBytes = 100L * 1024 * 1024;

  @Column(name = "video_chunk_upload_enabled", nullable = false)
  private boolean videoChunkUploadEnabled = true;

  @Column(name = "video_chunk_size_mb", nullable = false)
  private int videoChunkSizeMb = 5;

  @Column(nullable = false)
  private int dailyUploadCountLimit = 5000;

  @Column(name = "max_files_per_upload", nullable = false)
  private int maxFilesPerUpload = 30;

  @Column(nullable = false)
  private long userStorageQuotaBytes = 5L * 1024 * 1024 * 1024;

  @Column(nullable = false)
  private boolean registrationEnabled = false;

  /**
   * 注册邮箱验证开关（registration.email_verify_required，默认 false）：
   * 开启后新注册账号必须完成邮箱验证码校验（email_verified 置位）才算激活；关闭时注册即激活。
   * 生效前提：管理后台已配置并启用 SMTP，否则验证码无法发出。
   */
  @Column(name = "registration_email_verify_required", nullable = false)
  private boolean registrationEmailVerifyRequired = false;

  /**
   * 登录二步验证开关（login.totp_required，默认 false）：
   * 开启后所有用户登录在密码校验通过后需再输入 TOTP 动态口令（未绑定用户进入强制绑定流程）。
   */
  @Column(name = "login_totp_required", nullable = false)
  private boolean loginTotpRequired = false;

  @Column(name = "guest_like_enabled", nullable = false)
  private boolean guestLikeEnabled = true;

  @Column(name = "guest_upload_enabled", nullable = false)
  private boolean guestUploadEnabled = false;

  @Column(name = "auto_cleanup_days", nullable = false)
  private int autoCleanupDays = 30;

  @Column(name = "asset_domain", length = 255)
  private String assetDomain;

  @Lob
  @Column(name = "custom_footer_html", columnDefinition = "TEXT")
  private String customFooterHtml;

  @Column(name = "ai_moderation_enabled", nullable = false)
  private boolean aiModerationEnabled = false;

  @Column(name = "ai_labeling_enabled", nullable = false)
  private boolean aiLabelingEnabled = false;

  @Column(name = "ai_tencent_secret_id", length = 128)
  private String aiTencentSecretId;

  @Column(name = "ai_tencent_secret_key", length = 128)
  private String aiTencentSecretKey;

  @Column(name = "ai_tencent_region", length = 64)
  private String aiTencentRegion;

  @Column(name = "ai_tencent_bucket", length = 128)
  private String aiTencentBucket;

  @Column(name = "ai_tencent_detect_scenes", length = 128)
  private String aiTencentDetectScenes;

  @Column(name = "ai_moderation_block_confidence")
  private Integer aiModerationBlockConfidence = 90;

  @Column(name = "ai_moderation_review_confidence")
  private Integer aiModerationReviewConfidence = 60;

  @Column(name = "ai_label_min_confidence")
  private Integer aiLabelMinConfidence = 60;

  @Column(nullable = false, updatable = false)
  private Instant createdAt;

  @Column(nullable = false)
  private Instant updatedAt;

  private String updatedBy;

  @PrePersist
  void onCreate() {
    Instant now = Instant.now();
    this.createdAt = now;
    this.updatedAt = now;
  }

  @PreUpdate
  void onUpdate() {
    this.updatedAt = Instant.now();
  }
}

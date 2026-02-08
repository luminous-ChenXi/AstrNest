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

  @Column(nullable = false)
  private long userStorageQuotaBytes = 5L * 1024 * 1024 * 1024;

  @Column(nullable = false)
  private boolean registrationEnabled = false;

  @Column(name = "guest_like_enabled", nullable = false)
  private boolean guestLikeEnabled = true;

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

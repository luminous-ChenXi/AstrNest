package com.imgbed.system;

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
  private long maxUploadBytes = 5L * 1024 * 1024;

  @Column(nullable = false)
  private int dailyUploadCountLimit = 5000;

  @Column(nullable = false)
  private long userStorageQuotaBytes = 5L * 1024 * 1024 * 1024;

  @Column(nullable = false)
  private boolean registrationEnabled = false;

  @Column(name = "guest_like_enabled", nullable = false)
  private boolean guestLikeEnabled = true;

  @Column(name = "asset_domain", length = 255)
  private String assetDomain = "http://localhost:8080";

  @Lob
  @Column(name = "custom_footer_html", columnDefinition = "TEXT")
  private String customFooterHtml;

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

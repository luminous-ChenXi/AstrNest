package com.imgbed.chenxi.mail;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "chenxi_mail_config")
@Getter
@Setter
public class ChenxiMailConfig {

  @Id
  private Long id = 1L;

  @Column(name = "smtp_host", nullable = false, length = 200)
  private String smtpHost;

  @Column(name = "smtp_port", nullable = false)
  private Integer smtpPort;

  @Column(name = "smtp_username", nullable = false, length = 200)
  private String smtpUsername;

  @Column(name = "smtp_password", nullable = false, length = 200)
  private String smtpPassword;

  @Column(name = "secure_type", nullable = false, length = 20)
  private String secureType = "ssl";

  @Column(name = "from_email", nullable = false, length = 200)
  private String fromEmail;

  @Column(name = "from_name", nullable = false, length = 120)
  private String fromName;

  @Column(nullable = false)
  private boolean enabled = true;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Column(name = "updated_by", length = 120)
  private String updatedBy;

  @PrePersist
  @PreUpdate
  void onUpdate() {
    this.updatedAt = Instant.now();
  }
}

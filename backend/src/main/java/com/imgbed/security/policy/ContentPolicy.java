package com.imgbed.security.policy;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "content_policy")
@Getter
@Setter
public class ContentPolicy {

  @Id
  @Column(name = "policy_key", nullable = false, length = 32)
  private String key = "default";

  @Column(nullable = false)
  private boolean nsfwDetectionEnabled = true;

  @Column(nullable = false)
  private boolean violenceDetectionEnabled = true;

  @Column(nullable = false)
  private int manualReviewThreshold = 3;

  @Column(length = 255)
  private String webhookUrl;
}

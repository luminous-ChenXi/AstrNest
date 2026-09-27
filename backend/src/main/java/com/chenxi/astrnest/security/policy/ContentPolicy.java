package com.chenxi.astrnest.security.policy;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
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

  /**
   * 记录更新时间：应用侧初始化；列定义与 install-schema.sql 对齐（DB 默认值 + ON UPDATE），
   * 保证 Hibernate 建表与安装向导建表两种初始化顺序下原生种子 INSERT 均可用。
   */
  @Column(name = "updated_at", nullable = false,
      columnDefinition = "DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)")
  private Instant updatedAt = Instant.now();
}

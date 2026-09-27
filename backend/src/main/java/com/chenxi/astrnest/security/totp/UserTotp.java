package com.chenxi.astrnest.security.totp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/**
 * 用户 TOTP（二步验证）绑定：一个用户最多一条绑定记录。
 *
 * <p>安全口径：密钥明文 Base32 存库（与主流自建图床一致，受库访问权限保护）；
 * 还原码绝不存明文——逐条 BCrypt 哈希后以 JSON 数组存入 recovery_hashes，
 * 使用即焚（命中后从数组中移除）。</p>
 */
@Entity
@Table(name = "user_totp")
@Getter
@Setter
public class UserTotp {

  @Id
  private Long userId;

  /** Base32 编码的 TOTP 密钥（20 字节熵） */
  @Column(nullable = false, length = 64)
  private String secret;

  /** 是否已完成首次验证码确认（未确认的绑定在登录流程中不可用于换发 JWT） */
  @Column(nullable = false)
  private boolean confirmed = false;

  /** 还原码 BCrypt 哈希 JSON 数组（10 个 8 位码，一次性使用） */
  @Column(name = "recovery_hashes", columnDefinition = "TEXT")
  private String recoveryHashes;

  /** 最近一次成功验证的计数器（防同一时间窗内的口令重放） */
  @Column(name = "last_used_counter")
  private Long lastUsedCounter;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "confirmed_at")
  private Instant confirmedAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

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

package com.chenxi.astrnest.security.bruteforce;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
    name = "auth_lock_states",
    uniqueConstraints = {@UniqueConstraint(columnNames = {"username", "ip", "dimension"})}
)
@Getter
@Setter
public class AuthLockState {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 191)
  private String username = "";

  @Column(nullable = false, length = 64)
  private String ip = "";

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 24)
  private LockDimension dimension = LockDimension.USER_IP;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private LockStage stage = LockStage.INITIAL;

  @Column(nullable = false)
  private int failCount = 0;

  @Column(nullable = false)
  private int lockCount = 0;

  @Column
  private Instant lockedUntil;

  @Column
  private Instant lastFailedAt;

  @Column(length = 255)
  private String lockReason;

  @Column(length = 10)
  private String windowDate;
}

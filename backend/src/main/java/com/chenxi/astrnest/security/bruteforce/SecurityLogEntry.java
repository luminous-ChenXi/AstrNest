package com.chenxi.astrnest.security.bruteforce;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "security_logs")
@Getter
@Setter
public class SecurityLogEntry {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 64)
  private String eventType;

  @Column(length = 191)
  private String username;

  @Column(length = 64)
  private String ip;

  @Column(length = 512)
  private String message;

  @Column(nullable = false)
  private Instant createdAt = Instant.now();
}

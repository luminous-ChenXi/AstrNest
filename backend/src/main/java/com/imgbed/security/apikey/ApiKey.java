package com.imgbed.security.apikey;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "api_keys")
@Getter
@Setter
public class ApiKey {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 48)
  private String publicId;

  @Column(nullable = false, length = 120)
  private String name;

  @Column(length = 255)
  private String description;

  @Column(nullable = false)
  private String secretHash;

  @Column(nullable = false, length = 80)
  private String maskedKey;

  @Column(nullable = false)
  private boolean active = true;

  @Column(nullable = false, updatable = false)
  private Instant createdAt = Instant.now();

  private Instant lastUsedAt;

  @Column(nullable = false)
  private long requestCount = 0;

  @Column(nullable = false)
  private int dailyQuota = 1000;

  @Column(nullable = false)
  private int requestsToday = 0;

  private LocalDate lastRequestDate;
}

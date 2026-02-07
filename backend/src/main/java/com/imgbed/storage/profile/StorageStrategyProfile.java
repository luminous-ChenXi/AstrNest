package com.imgbed.storage.profile;

import com.imgbed.storage.StorageStrategy;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "storage_strategy_profiles")
@Getter
@Setter
public class StorageStrategyProfile {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  private StorageStrategy strategy;

  @Column(nullable = false, length = 120, unique = true)
  private String name;

  @Column(nullable = false, length = 120)
  private String displayName;

  @Column(length = 255)
  private String description;

  @Column(nullable = false)
  private boolean active = false;

  @Column(nullable = false)
  private boolean enabled = true;

  @Lob
  @Column(name = "config_json", columnDefinition = "LONGTEXT")
  private String configJson;

  @Column(length = 120)
  private String createdBy;

  @Column(length = 120)
  private String updatedBy;

  @Column(nullable = false, updatable = false)
  private Instant createdAt;

  @Column(nullable = false)
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

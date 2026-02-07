package com.imgbed.security.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
public class UserAccount {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 64)
  private String username;

  @Column(nullable = false, length = 120)
  private String password;

  @Column(name = "nickname", nullable = false, length = 120)
  private String displayName;

  @Column(length = 180)
  private String email;

  @Column(length = 512)
  private String avatarUrl;

  @Column(length = 255)
  private String website;

  @Column(length = 255)
  private String signature;

  @Column(length = 120)
  private String location;

  @Column(length = 1024)
  private String loginIpHistory;

  @Column(length = 64)
  private String lastLoginIp;

  private Instant lastLoginAt;

  @Column(nullable = false)
  private boolean active = true;

  @Column(name = "daily_upload_limit")
  private Integer dailyUploadLimit = 100;

  @Column(name = "storage_quota_mb")
  private Long storageQuotaMb = 200L;

  @Column(nullable = false, updatable = false)
  private Instant createdAt = Instant.now();

  @ManyToMany(fetch = FetchType.EAGER)
  @JoinTable(
      name = "user_roles",
      joinColumns = @JoinColumn(name = "user_id"),
      inverseJoinColumns = @JoinColumn(name = "role_id")
  )
  private Set<UserRole> roles = new HashSet<>();
}

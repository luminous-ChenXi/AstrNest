package com.chenxi.astrnest.security.user;

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

  /**
   * 外部身份源唯一标识（OIDC/OAuth2.1 的 sub），影子账号专用；本地账号恒为 null。
   * 仅在 chenxi.passport.enabled 开启且外部身份源登录时写入（见 passport 包）。
   */
  @Column(name = "sso_sub", unique = true, length = 64)
  private String ssoSub;

  /**
   * 身份来源：local=本地注册/本地登录（默认），passport=外部 SSO 身份源（通行证类，泛化即 sso）。
   * 非 local 的账号资料由身份源侧维护，本地资料修改与改密被拒绝（见 UserPortalService）。
   */
  @Column(name = "identity_source", nullable = false, length = 32)
  private String identitySource = "local";

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

  /**
   * 邮箱是否已验证（registration.email_verify_required=true 时注册须完成验证码校验才置位）。
   * 注册路径必须显式赋值；存量库由 SchemaAlignmentRunner 以已验证（历史流程本就强制验证码）补列。
   */
  @Column(name = "email_verified", nullable = false)
  private boolean emailVerified = false;

  @Column(name = "daily_upload_limit")
  private Integer dailyUploadLimit = 100;

  @Column(name = "storage_quota_mb")
  private Long storageQuotaMb = 200L;

  @Column(nullable = false, updatable = false)
  private Instant createdAt = Instant.now();

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
      name = "user_roles",
      joinColumns = @JoinColumn(name = "user_id"),
      inverseJoinColumns = @JoinColumn(name = "role_id")
  )
  private Set<UserRole> roles = new HashSet<>();
}

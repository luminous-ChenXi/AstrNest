package com.chenxi.astrnest.security.config;

import com.chenxi.astrnest.security.policy.ContentPolicy;
import com.chenxi.astrnest.security.policy.ContentPolicyRepository;
import com.chenxi.astrnest.security.user.UserAccount;
import com.chenxi.astrnest.security.user.UserAccountRepository;
import com.chenxi.astrnest.security.user.UserRole;
import com.chenxi.astrnest.security.user.UserRoleRepository;
import jakarta.annotation.PostConstruct;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminAccountInitializer {

  private final AdminAccountProperties adminAccountProperties;
  private final UserAccountRepository userAccountRepository;
  private final UserRoleRepository userRoleRepository;
  private final PasswordEncoder passwordEncoder;
  private final ContentPolicyRepository contentPolicyRepository;

  @PostConstruct
  public void bootstrapAdmin() {
    UserRole adminRole = userRoleRepository.findByName("ADMIN")
        .orElseGet(() -> saveRole("ADMIN", "超级管理员"));
    userRoleRepository.findByName("USER")
        .orElseGet(() -> saveRole("USER", "普通用户"));
    userRoleRepository.findByName("GUEST")
        .orElseGet(() -> saveRole("GUEST", "受限访客"));

    userAccountRepository.findByUsername(adminAccountProperties.getUsername())
        .ifPresentOrElse(
            user -> log.info("Admin account {} already exists", user.getUsername()),
            () -> {
              UserAccount admin = new UserAccount();
              admin.setUsername(adminAccountProperties.getUsername());
              admin.setDisplayName(adminAccountProperties.getDisplayName());
              admin.setEmail(adminAccountProperties.getEmail());
              admin.setPassword(passwordEncoder.encode(adminAccountProperties.getPassword()));
              admin.setDailyUploadLimit(null);
              admin.setStorageQuotaMb(null);
              admin.setRoles(new java.util.HashSet<>(Set.of(adminRole)));
              userAccountRepository.save(admin);
              log.warn("Initialized default admin account: {}", admin.getUsername());
            }
        );

    contentPolicyRepository.findById("default")
        .orElseGet(() -> {
          ContentPolicy policy = new ContentPolicy();
          policy.setKey("default");
          policy.setNsfwDetectionEnabled(true);
          policy.setViolenceDetectionEnabled(true);
          policy.setManualReviewThreshold(2);
          return contentPolicyRepository.save(policy);
        });
  }

  private UserRole saveRole(String name, String description) {
    UserRole role = new UserRole();
    role.setName(name);
    role.setDescription(description);
    return userRoleRepository.save(role);
  }
}

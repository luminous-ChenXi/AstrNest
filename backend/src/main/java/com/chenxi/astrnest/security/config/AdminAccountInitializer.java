package com.chenxi.astrnest.security.config;

import com.chenxi.astrnest.security.policy.ContentPolicy;
import com.chenxi.astrnest.security.policy.ContentPolicyRepository;
import com.chenxi.astrnest.security.user.UserRole;
import com.chenxi.astrnest.security.user.UserRoleRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminAccountInitializer {

  private final UserRoleRepository userRoleRepository;
  private final ContentPolicyRepository contentPolicyRepository;

  @PostConstruct
  public void bootstrapAdmin() {
    userRoleRepository.findByName("ADMIN")
        .orElseGet(() -> saveRole("ADMIN", "超级管理员"));
    userRoleRepository.findByName("USER")
        .orElseGet(() -> saveRole("USER", "普通用户"));
    userRoleRepository.findByName("GUEST")
        .orElseGet(() -> saveRole("GUEST", "受限访客"));

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

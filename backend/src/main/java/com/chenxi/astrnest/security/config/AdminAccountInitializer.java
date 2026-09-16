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
    try {
      doBootstrap();
    } catch (Exception exception) {
      // 全新部署（schema 未安装）时 roles/content_policy 表可能尚不存在，
      // 此时不允许让应用启动失败——安装向导会通过 install-schema.sql 建表并补种角色。
      log.warn("Skipped role/content-policy bootstrap because schema is not ready yet: {}. "
          + "It will be satisfied by the install wizard (install-schema.sql) or on next restart.",
          exception.getMessage());
    }
  }

  private void doBootstrap() {
    userRoleRepository.findByName("ADMIN")
        .orElseGet(() -> saveRole("ADMIN", "超级管理员"));
    userRoleRepository.findByName("USER")
        .orElseGet(() -> saveRole("USER", "普通用户"));

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

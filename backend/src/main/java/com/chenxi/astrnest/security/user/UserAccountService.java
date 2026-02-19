package com.chenxi.astrnest.security.user;

import com.chenxi.astrnest.security.dto.UserProfileResponse;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserAccountService {

  private final UserAccountRepository userAccountRepository;

  public UserProfileResponse getCurrentProfile() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      throw new IllegalStateException("当前没有登录用户");
    }
    String username = authentication.getName();
    UserAccount user = userAccountRepository.findByUsername(username)
        .orElseThrow(() -> new IllegalStateException("未找到用户"));
    Set<String> roles = user.getRoles().stream()
        .map(UserRole::getName)
        .collect(Collectors.toSet());
    return new UserProfileResponse(
        user.getId(),
        user.getUsername(),
        user.getDisplayName(),
        user.getEmail(),
        user.getAvatarUrl(),
        user.getWebsite(),
        user.getSignature(),
        user.getLocation(),
        user.getLoginIpHistory(),
        user.getLastLoginIp(),
        user.getLastLoginAt(),
        roles
    );
  }
}

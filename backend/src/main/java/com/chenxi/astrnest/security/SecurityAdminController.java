package com.chenxi.astrnest.security;

import com.chenxi.astrnest.security.dto.ContentPolicyRequest;
import com.chenxi.astrnest.security.dto.UserProfileResponse;
import com.chenxi.astrnest.security.policy.ContentPolicy;
import com.chenxi.astrnest.security.policy.ContentPolicyService;
import com.chenxi.astrnest.security.user.UserAccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/security")
@RequiredArgsConstructor
@Validated
public class SecurityAdminController {

  private final UserAccountService userAccountService;
  private final ContentPolicyService contentPolicyService;

  @GetMapping("/profile")
  public UserProfileResponse profile() {
    return userAccountService.getCurrentProfile();
  }

  @GetMapping("/policy")
  @PreAuthorize("hasRole('ADMIN')")
  public ContentPolicy policy() {
    return contentPolicyService.currentPolicy();
  }

  @PutMapping("/policy")
  @PreAuthorize("hasRole('ADMIN')")
  public ContentPolicy updatePolicy(@Valid @RequestBody ContentPolicyRequest request) {
    return contentPolicyService.update(request);
  }
}

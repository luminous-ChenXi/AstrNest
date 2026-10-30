package com.chenxi.astrnest.user;

import com.chenxi.astrnest.security.apikey.ApiKeyProperties;
import com.chenxi.astrnest.security.dto.UserProfileResponse;
import com.chenxi.astrnest.user.dto.ChangePasswordRequest;
import com.chenxi.astrnest.user.dto.DeleteUploadsRequest;
import com.chenxi.astrnest.user.dto.ToggleUploadLikeResponse;
import com.chenxi.astrnest.user.dto.UpdateProfileRequest;
import com.chenxi.astrnest.user.dto.UpdateUploadVisibilityRequest;
import com.chenxi.astrnest.user.dto.UserOverviewResponse;
import com.chenxi.astrnest.user.dto.UserProfileDetailResponse;
import com.chenxi.astrnest.user.dto.UserSecuritySettingsResponse;
import com.chenxi.astrnest.user.dto.UserUploadDetailResponse;
import com.chenxi.astrnest.user.dto.UserUploadPageResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','USER')")
public class UserPortalController {

  /** 改密端点进程内限流：口径与登录类端点一致（30 次/分钟/IP），兜底 BCrypt 在线爆破 */
  private static final int PASSWORD_CHANGE_LIMIT_PER_MINUTE = 30;
  private static final java.time.Duration PASSWORD_CHANGE_RATE_WINDOW = java.time.Duration.ofMinutes(1);

  private final UserPortalService userPortalService;
  private final ApiKeyProperties apiKeyProperties;
  private final com.chenxi.astrnest.common.ClientIpResolver clientIpResolver;
  private final com.chenxi.astrnest.common.InMemoryRateLimiter rateLimiter;

  @GetMapping("/overview")
  public UserOverviewResponse overview() {
    return userPortalService.overview(5);
  }

  @GetMapping("/uploads")
  public UserUploadPageResponse uploads(@RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "12") int size) {
    return userPortalService.uploads(page, size);
  }

  @GetMapping("/uploads/{id}")
  public UserUploadDetailResponse uploadDetail(@PathVariable Long id) {
    return userPortalService.uploadDetail(id);
  }

  @PostMapping("/uploads/{id}/like")
  public ToggleUploadLikeResponse toggleLike(@PathVariable Long id) {
    return userPortalService.toggleLike(id);
  }

  @PutMapping("/uploads/{id}/visibility")
  public UserUploadDetailResponse updateVisibility(@PathVariable Long id,
      @Valid @RequestBody UpdateUploadVisibilityRequest request) {
    return userPortalService.updateVisibility(id, request);
  }

  @DeleteMapping("/uploads/{id}")
  public void deleteUpload(@PathVariable Long id) {
    userPortalService.deleteUpload(id);
  }

  @PostMapping("/uploads/batch-delete")
  public void deleteUploads(@Valid @RequestBody DeleteUploadsRequest request) {
    userPortalService.deleteUploads(request);
  }

  @GetMapping("/profile")
  public UserProfileDetailResponse profile() {
    return userPortalService.profile();
  }

  @PutMapping("/profile")
  public UserProfileResponse updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
    return userPortalService.updateProfile(request);
  }

  @PostMapping("/security/password")
  public void changePassword(@Valid @RequestBody ChangePasswordRequest request, HttpServletRequest httpRequest) {
    String ip = clientIpResolver.resolve(httpRequest);
    if (rateLimiter.tryAcquire("pwd-change:" + ip, PASSWORD_CHANGE_LIMIT_PER_MINUTE, PASSWORD_CHANGE_RATE_WINDOW)) {
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.TOO_MANY_REQUESTS, "请求过于频繁，请稍后再试");
    }
    userPortalService.changePassword(request, ip);
  }

  @GetMapping("/security/settings")
  public UserSecuritySettingsResponse securitySettings() {
    return userPortalService.securitySettings(apiKeyProperties.getHeaderName(), apiKeyProperties.getDefaultDailyQuota());
  }
}

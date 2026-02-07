package com.imgbed.user;

import com.imgbed.security.apikey.ApiKeyProperties;
import com.imgbed.security.dto.UserProfileResponse;
import com.imgbed.user.dto.ChangePasswordRequest;
import com.imgbed.user.dto.DeleteUploadsRequest;
import com.imgbed.user.dto.ToggleUploadLikeResponse;
import com.imgbed.user.dto.UpdateProfileRequest;
import com.imgbed.user.dto.UpdateUploadVisibilityRequest;
import com.imgbed.user.dto.UserOverviewResponse;
import com.imgbed.user.dto.UserProfileDetailResponse;
import com.imgbed.user.dto.UserSecuritySettingsResponse;
import com.imgbed.user.dto.UserUploadDetailResponse;
import com.imgbed.user.dto.UserUploadPageResponse;
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

  private final UserPortalService userPortalService;
  private final ApiKeyProperties apiKeyProperties;

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
  public void changePassword(@Valid @RequestBody ChangePasswordRequest request) {
    userPortalService.changePassword(request);
  }

  @GetMapping("/security/settings")
  public UserSecuritySettingsResponse securitySettings() {
    return userPortalService.securitySettings(apiKeyProperties.getHeaderName(), apiKeyProperties.getDefaultDailyQuota());
  }
}

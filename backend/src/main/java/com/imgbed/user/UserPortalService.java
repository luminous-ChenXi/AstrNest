package com.imgbed.user;

import com.imgbed.security.dto.UserProfileResponse;
import com.imgbed.security.user.UserAccount;
import com.imgbed.security.user.UserAccountRepository;
import com.imgbed.security.user.UserRole;
import com.imgbed.upload.record.UploadRecord;
import com.imgbed.upload.record.UploadRecordService;
import com.imgbed.user.dto.ChangePasswordRequest;
import com.imgbed.user.dto.DeleteUploadsRequest;
import com.imgbed.user.dto.LoginHistoryEntry;
import com.imgbed.user.dto.ToggleUploadLikeResponse;
import com.imgbed.user.dto.UpdateProfileRequest;
import com.imgbed.user.dto.UpdateUploadVisibilityRequest;
import com.imgbed.user.dto.UserOverviewResponse;
import com.imgbed.user.dto.UserProfileDetailResponse;
import com.imgbed.user.dto.UserSecuritySettingsResponse;
import com.imgbed.user.dto.UserUploadDetailResponse;
import com.imgbed.user.dto.UserUploadItemResponse;
import com.imgbed.user.dto.UserUploadPageResponse;
import com.imgbed.user.login.UserLoginEventService;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class UserPortalService {

  private final UserAccountRepository userAccountRepository;
  private final UploadRecordService uploadRecordService;
  private final PasswordEncoder passwordEncoder;
  private final UserLoginEventService userLoginEventService;

  public UserOverviewResponse overview(int latestCount) {
    UserAccount user = currentUser();
    long totalUploads = uploadRecordService.countTotalForUser(user.getId());
    long todayUploads = uploadRecordService.countTodayForUser(user.getId());
    long storageBytes = uploadRecordService.totalSizeForUser(user.getId());
    Page<UploadRecord> latest = uploadRecordService.findForUser(user.getId(), PageRequest.of(0, latestCount));
    List<UserUploadItemResponse> latestUploads = latest.stream()
        .map(this::toUploadItem)
        .toList();
    Integer dailyLimit = sanitizedLimit(user.getDailyUploadLimit());
    int dailyRemaining = dailyLimit == null ? -1 : Math.max(dailyLimit - (int) todayUploads, 0);
    Long storageQuotaMb = sanitizedQuota(user.getStorageQuotaMb());
    long storageRemainingBytes = storageQuotaMb == null
        ? -1
        : Math.max((storageQuotaMb * 1024L * 1024L) - storageBytes, 0);
    return new UserOverviewResponse(
        totalUploads,
        todayUploads,
        storageBytes,
        dailyLimit,
        dailyRemaining,
        storageQuotaMb,
        storageRemainingBytes,
        latestUploads
    );
  }

  public UserUploadPageResponse uploads(int page, int size) {
    UserAccount user = currentUser();
    Page<UploadRecord> result = uploadRecordService.findForUser(user.getId(), PageRequest.of(page, size));
    List<UserUploadItemResponse> items = result.stream().map(this::toUploadItem).toList();
    return new UserUploadPageResponse(items, result.getTotalElements(), page, size);
  }

  public UserUploadDetailResponse uploadDetail(Long uploadId) {
    UserAccount user = currentUser();
    UploadRecord record = uploadRecordService.requireOwnedRecord(uploadId, user.getId());
    boolean liked = uploadRecordService.isLikedBy(record.getId(), user.getId());
    return toUploadDetail(record, liked);
  }

  public ToggleUploadLikeResponse toggleLike(Long uploadId) {
    UserAccount user = currentUser();
    UploadRecord record = uploadRecordService.requireOwnedRecord(uploadId, user.getId());
    UploadRecordService.LikeMutation mutation = uploadRecordService.toggleLike(record, user);
    return new ToggleUploadLikeResponse(mutation.likeCount(), mutation.liked());
  }

  public UserUploadDetailResponse updateVisibility(Long uploadId, UpdateUploadVisibilityRequest request) {
    UserAccount user = currentUser();
    UploadRecord record = uploadRecordService.requireOwnedRecord(uploadId, user.getId());
    uploadRecordService.updateVisibility(record, Boolean.TRUE.equals(request.publicAccessible()));
    boolean liked = uploadRecordService.isLikedBy(record.getId(), user.getId());
    return toUploadDetail(record, liked);
  }

  public void deleteUpload(Long id) {
    UserAccount user = currentUser();
    uploadRecordService.deleteUserRecord(id, user.getId());
  }

  public void deleteUploads(DeleteUploadsRequest request) {
    UserAccount user = currentUser();
    long[] ids = request.ids();
    if (ids == null || ids.length == 0) {
      return;
    }
    for (long id : ids) {
      uploadRecordService.deleteUserRecord(id, user.getId());
    }
  }

  public UserProfileDetailResponse profile() {
    UserAccount user = currentUser();
    return new UserProfileDetailResponse(
        user.getId(),
        user.getUsername(),
        user.getDisplayName(),
        user.getEmail(),
        user.getAvatarUrl(),
        user.getWebsite(),
        user.getSignature(),
        user.getLocation(),
        user.isActive(),
        user.getCreatedAt(),
        user.getLoginIpHistory(),
        user.getLastLoginIp(),
        user.getLastLoginAt(),
        roles(user)
    );
  }

  public UserProfileResponse updateProfile(UpdateProfileRequest request) {
    UserAccount user = currentUser();
    user.setDisplayName(request.displayName());
    user.setAvatarUrl(cleanText(request.avatarUrl()));
    user.setWebsite(cleanText(request.website()));
    user.setSignature(cleanText(request.signature()));
    user.setLocation(cleanText(request.location()));
    userAccountRepository.save(user);
    return profileResponse(user);
  }

  public void changePassword(ChangePasswordRequest request) {
    if (!request.newPassword().equals(request.confirmPassword())) {
      throw new IllegalArgumentException("两次输入的密码不一致");
    }
    UserAccount user = currentUser();
    if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
      throw new BadCredentialsException("当前密码不正确");
    }
    user.setPassword(passwordEncoder.encode(request.newPassword()));
    userAccountRepository.save(user);
  }

  public UserSecuritySettingsResponse securitySettings(String apiHeaderName, int defaultQuota) {
    UserAccount user = currentUser();
    List<LoginHistoryEntry> history = userLoginEventService.latestForUser(user.getId(), 5);
    return new UserSecuritySettingsResponse(apiHeaderName, defaultQuota, history);
  }

  private UserUploadItemResponse toUploadItem(UploadRecord record) {
    return new UserUploadItemResponse(
        record.getId(),
        record.getFileName(),
        record.getPublicUrl(),
        record.getSize(),
        record.getReviewStatus(),
        record.getUploadedAt(),
        record.isPublicAccessible(),
        record.getLikeCount(),
        record.getInvokeCount(),
        record.getObjectKey(),
        record.getStorageProvider()
    );
  }

  private UserUploadDetailResponse toUploadDetail(UploadRecord record, boolean likedByMe) {
    return new UserUploadDetailResponse(
        record.getId(),
        record.getFileName(),
        record.getPublicUrl(),
        record.getSize(),
        record.getReviewStatus(),
        record.getUploadedAt(),
        record.isPublicAccessible(),
        record.getLikeCount(),
        record.getInvokeCount(),
        record.getObjectKey(),
        record.getStorageProvider(),
        record.getStorageFullPath(),
        record.getUploaderIp(),
        likedByMe
    );
  }

  private UserAccount currentUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !(authentication.getPrincipal() instanceof UserDetails userDetails)) {
      throw new BadCredentialsException("未登录或会话已失效");
    }
    return userAccountRepository.findByUsername(userDetails.getUsername())
        .orElseThrow(() -> new BadCredentialsException("未找到用户"));
  }

  private Set<String> roles(UserAccount user) {
    return user.getRoles().stream().map(UserRole::getName).collect(Collectors.toSet());
  }

  private UserProfileResponse profileResponse(UserAccount user) {
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
        roles(user)
    );
  }

  private String cleanText(String value) {
    return StringUtils.hasText(value) ? value.trim() : null;
  }

  private Integer sanitizedLimit(Integer value) {
    if (value == null || value <= 0) {
      return null;
    }
    return value;
  }

  private Long sanitizedQuota(Long value) {
    if (value == null || value <= 0) {
      return null;
    }
    return value;
  }
}

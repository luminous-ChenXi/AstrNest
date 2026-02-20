package com.chenxi.astrnest.user;

import com.chenxi.astrnest.ai.AiLabel;
import com.chenxi.astrnest.security.dto.UserProfileResponse;
import com.chenxi.astrnest.security.user.UserAccount;
import com.chenxi.astrnest.security.user.UserAccountRepository;
import com.chenxi.astrnest.security.user.UserRole;
import com.chenxi.astrnest.storage.PublicAssetUrlResolver;
import com.chenxi.astrnest.tag.dto.ChenxiTagResponse;
import com.chenxi.astrnest.upload.dto.AiReviewFeedback;
import com.chenxi.astrnest.upload.media.MediaCategory;
import com.chenxi.astrnest.upload.record.UploadRecord;
import com.chenxi.astrnest.upload.record.UploadRecordRepository;
import com.chenxi.astrnest.upload.record.UploadRecordService;
import com.chenxi.astrnest.user.dto.ChangePasswordRequest;
import com.chenxi.astrnest.user.dto.DeleteUploadsRequest;
import com.chenxi.astrnest.user.dto.LoginHistoryEntry;
import com.chenxi.astrnest.user.dto.ToggleUploadLikeResponse;
import com.chenxi.astrnest.user.dto.UpdateProfileRequest;
import com.chenxi.astrnest.user.dto.UpdateUploadVisibilityRequest;
import com.chenxi.astrnest.user.dto.UserOverviewResponse;
import com.chenxi.astrnest.user.dto.UserProfileDetailResponse;
import com.chenxi.astrnest.user.dto.UserSecuritySettingsResponse;
import com.chenxi.astrnest.user.dto.UserUploadDetailResponse;
import com.chenxi.astrnest.user.dto.UserUploadItemResponse;
import com.chenxi.astrnest.user.dto.UserUploadPageResponse;
import com.chenxi.astrnest.user.login.UserLoginEventService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserPortalService {

  private final UserAccountRepository userAccountRepository;
  private final UploadRecordRepository uploadRecordRepository;
  private final UploadRecordService uploadRecordService;
  private final PasswordEncoder passwordEncoder;
  private final UserLoginEventService userLoginEventService;
  private final PublicAssetUrlResolver publicAssetUrlResolver;
  private final ObjectMapper objectMapper;

  @Transactional(readOnly = true)
  public UserOverviewResponse overview(int latestCount) {
    UserAccount user = currentUser();
    long totalUploads = uploadRecordService.countTotalForUser(user.getId());
    long todayUploads = uploadRecordService.countTodayForUser(user.getId());
    long storageBytes = uploadRecordService.totalSizeForUser(user.getId());
    Page<UploadRecord> latest = uploadRecordService.findForUser(user.getId(), PageRequest.of(0, latestCount));
    List<Long> latestIds = latest.stream().map(UploadRecord::getId).toList();
    Map<Long, List<ChenxiTagResponse>> latestTagMap = resolveTags(latestIds);
    List<UserUploadItemResponse> latestUploads = latest.stream()
        .map(record -> toUploadItem(record, latestTagMap.getOrDefault(record.getId(), List.of())))
        .toList();
    Integer dailyLimit = sanitizedLimit(user.getDailyUploadLimit());
    int totalRemaining = dailyLimit == null ? -1 : Math.max(dailyLimit - (int) totalUploads, 0);
    Long storageQuotaMb = sanitizedQuota(user.getStorageQuotaMb());
    long storageRemainingBytes = storageQuotaMb == null
        ? -1
        : Math.max((storageQuotaMb * 1024L * 1024L) - storageBytes, 0);
    return new UserOverviewResponse(
        totalUploads,
        todayUploads,
        storageBytes,
        dailyLimit,
        totalRemaining,
        storageQuotaMb,
        storageRemainingBytes,
        latestUploads
    );
  }

  @Transactional(readOnly = true)
  public UserUploadPageResponse uploads(int page, int size) {
    UserAccount user = currentUser();
    Page<UploadRecord> result = uploadRecordService.findForUser(user.getId(), PageRequest.of(page, size));
    List<Long> ids = result.stream().map(UploadRecord::getId).toList();
    Map<Long, List<ChenxiTagResponse>> tagMap = resolveTags(ids);
    List<UserUploadItemResponse> items = result.stream()
        .map(record -> toUploadItem(record, tagMap.getOrDefault(record.getId(), List.of())))
        .toList();
    return new UserUploadPageResponse(items, result.getTotalElements(), page, size);
  }

  @Transactional(readOnly = true)
  public UserUploadDetailResponse uploadDetail(Long uploadId) {
    UserAccount user = currentUser();
    UploadRecord record = uploadRecordService.requireOwnedRecord(uploadId, user.getId());
    boolean liked = uploadRecordService.isLikedBy(record.getId(), user.getId());
    Map<Long, List<ChenxiTagResponse>> tagMap = resolveTags(List.of(record.getId()));
    return toUploadDetail(record, liked, tagMap.getOrDefault(record.getId(), List.of()));
  }

  @Transactional
  public ToggleUploadLikeResponse toggleLike(Long uploadId) {
    UserAccount user = currentUser();
    UploadRecord record = uploadRecordService.requireOwnedRecord(uploadId, user.getId());
    UploadRecordService.LikeMutation mutation = uploadRecordService.toggleLike(record, user);
    return new ToggleUploadLikeResponse(mutation.likeCount(), mutation.liked());
  }

  @Transactional
  public UserUploadDetailResponse updateVisibility(Long uploadId, UpdateUploadVisibilityRequest request) {
    UserAccount user = currentUser();
    UploadRecord record = uploadRecordService.requireOwnedRecord(uploadId, user.getId());
    uploadRecordService.updateVisibility(record, Boolean.TRUE.equals(request.publicAccessible()));
    boolean liked = uploadRecordService.isLikedBy(record.getId(), user.getId());
    Map<Long, List<ChenxiTagResponse>> tagMap = resolveTags(List.of(record.getId()));
    return toUploadDetail(record, liked, tagMap.getOrDefault(record.getId(), List.of()));
  }

  @Transactional
  public void deleteUpload(Long id) {
    UserAccount user = currentUser();
    uploadRecordService.deleteUserRecord(id, user.getId());
  }

  @Transactional
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

  @Transactional(readOnly = true)
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

  @Transactional
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

  @Transactional
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

  @Transactional(readOnly = true)
  public UserSecuritySettingsResponse securitySettings(String apiHeaderName, int defaultQuota) {
    UserAccount user = currentUser();
    List<LoginHistoryEntry> history = userLoginEventService.latestForUser(user.getId(), 5);
    return new UserSecuritySettingsResponse(apiHeaderName, defaultQuota, history);
  }

  private UserUploadItemResponse toUploadItem(UploadRecord record, List<ChenxiTagResponse> tags) {
    return new UserUploadItemResponse(
        record.getId(),
        record.getFileName(),
        record.getMediaUuid(),
        record.getMediaCategory() != null ? record.getMediaCategory().name() : MediaCategory.IMAGE.name(),
        publicAssetUrlResolver.resolve(record),
        resolvedThumbnailUrl(record),
        record.getEmbedUrl(),
        record.getSize(),
        record.getReviewStatus(),
        record.getUploadedAt(),
        record.isPublicAccessible(),
        record.getLikeCount(),
        record.getInvokeCount(),
        record.getObjectKey(),
        record.getStorageProvider(),
        tags,
        buildAiReviewFeedback(record)
    );
  }

  private UserUploadDetailResponse toUploadDetail(UploadRecord record, boolean likedByMe, List<ChenxiTagResponse> tags) {
    return new UserUploadDetailResponse(
        record.getId(),
        record.getFileName(),
        record.getMediaUuid(),
        record.getMediaCategory() != null ? record.getMediaCategory().name() : MediaCategory.IMAGE.name(),
        publicAssetUrlResolver.resolve(record),
        resolvedThumbnailUrl(record),
        record.getEmbedUrl(),
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
        tags,
        likedByMe,
        buildAiReviewFeedback(record)
    );
  }

  private Map<Long, List<ChenxiTagResponse>> resolveTags(List<Long> uploadIds) {
    Map<Long, List<ChenxiTagResponse>> tagMap = new HashMap<>();
    if (uploadIds == null || uploadIds.isEmpty()) {
      return tagMap;
    }
    uploadRecordRepository.findWithTagsByIdIn(uploadIds)
        .forEach(record -> {
          List<ChenxiTagResponse> tags = record.getTags() == null ? List.of() : record.getTags().stream()
              .map(tag -> new ChenxiTagResponse(tag.getId(), tag.getName(), tag.getSlug(), tag.getDescription()))
              .toList();
          tagMap.put(record.getId(), tags);
        });
    return tagMap;
  }

  private AiReviewFeedback buildAiReviewFeedback(UploadRecord record) {
    if (record == null) {
      return new AiReviewFeedback(null, List.of(), null, null, null);
    }
    return new AiReviewFeedback(
        record.getAiDecision(),
        parseAiLabels(record.getAiLabelSnapshot()),
        record.getAiErrorCode(),
        record.getAiErrorMessage(),
        record.getAiRequestId()
    );
  }

  private List<AiLabel> parseAiLabels(String snapshot) {
    if (!StringUtils.hasText(snapshot)) {
      return List.of();
    }
    try {
      return objectMapper.readValue(snapshot, new TypeReference<List<AiLabel>>() { });
    } catch (JsonProcessingException exception) {
      log.warn("解析 AI 标签失败：{}", exception.getMessage());
      return List.of();
    }
  }

  private String resolvedThumbnailUrl(UploadRecord record) {
    if (record == null) {
      return null;
    }
    String thumbnail = record.getThumbnailUrl();
    if (StringUtils.hasText(thumbnail)) {
      if (isAbsoluteUrl(thumbnail)) {
        return thumbnail;
      }
      return publicAssetUrlResolver.buildLocalPublicUrl(thumbnail);
    }
    if (StringUtils.hasText(record.getThumbnailStoragePath())) {
      return publicAssetUrlResolver.buildLocalPublicUrl(record.getThumbnailStoragePath());
    }
    if (record.getMediaCategory() != MediaCategory.VIDEO) {
      return publicAssetUrlResolver.resolve(record);
    }
    return null;
  }

  private boolean isAbsoluteUrl(String value) {
    if (!StringUtils.hasText(value)) {
      return false;
    }
    String normalized = value.toLowerCase(Locale.ROOT);
    return normalized.startsWith("http://") || normalized.startsWith("https://");
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

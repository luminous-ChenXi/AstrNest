package com.imgbed.gallery;

import com.imgbed.gallery.dto.PublicGalleryItemResponse;
import com.imgbed.gallery.dto.PublicGalleryPageResponse;
import com.imgbed.gallery.dto.PublicRecentLikeResponse;
import com.imgbed.security.user.UserAccount;
import com.imgbed.security.user.UserAccountRepository;
import com.imgbed.system.SystemConfigService;
import com.imgbed.upload.like.UploadLike;
import com.imgbed.upload.like.UploadLikeRepository;
import com.imgbed.upload.record.UploadRecord;
import com.imgbed.upload.record.UploadRecordRepository;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class PublicGalleryService {

  private final UploadRecordRepository uploadRecordRepository;
  private final UploadLikeRepository uploadLikeRepository;
  private final UserAccountRepository userAccountRepository;
  private final SystemConfigService systemConfigService;

  @Transactional(readOnly = true)
  public PublicGalleryPageResponse list(int page, int size, Authentication authentication, String visitorToken) {
    int normalizedPage = Math.max(page, 0);
    int normalizedSize = Math.min(Math.max(size, 1), 24);
    Pageable pageable = PageRequest.of(normalizedPage, normalizedSize, Sort.by(Sort.Direction.DESC, "uploadedAt"));
    Page<UploadRecord> result = uploadRecordRepository.findByPublicAccessibleTrueAndViolationFalse(pageable);
    List<UploadRecord> records = result.getContent();
    List<Long> recordIds = records.stream().map(UploadRecord::getId).toList();

    UserAccount viewer = resolveUser(authentication);
    String sanitizedVisitorToken = sanitizeVisitorToken(visitorToken);
    Set<Long> likedIds = resolveLikedIds(recordIds, viewer, sanitizedVisitorToken);
    Map<Long, PublicRecentLikeResponse> latestLikeMap = buildLatestLikeMap(recordIds);
    boolean guestLikeEnabled = systemConfigService.isGuestLikeEnabled();

    List<PublicGalleryItemResponse> items = records.stream()
        .map(record -> mapItem(record, likedIds.contains(record.getId()), latestLikeMap.get(record.getId())))
        .toList();

    return new PublicGalleryPageResponse(
        items,
        result.getTotalElements(),
        result.getTotalPages(),
        result.getNumber(),
        result.getSize(),
        guestLikeEnabled
    );
  }

  private Map<Long, PublicRecentLikeResponse> buildLatestLikeMap(List<Long> uploadIds) {
    Map<Long, PublicRecentLikeResponse> latest = new HashMap<>();
    if (uploadIds.isEmpty()) {
      return latest;
    }
    uploadLikeRepository.findLatestLikesForUploads(uploadIds)
        .forEach(like -> latest.computeIfAbsent(like.getUploadRecord().getId(), id -> toRecentLike(like)));
    return latest;
  }

  private Set<Long> resolveLikedIds(List<Long> uploadIds, UserAccount viewer, String visitorToken) {
    if (uploadIds.isEmpty()) {
      return Set.of();
    }
    if (viewer != null) {
      return new HashSet<>(uploadLikeRepository.findLikedRecordIdsByUserId(uploadIds, viewer.getId()));
    }
    if (StringUtils.hasText(visitorToken)) {
      return new HashSet<>(uploadLikeRepository.findLikedRecordIdsByGuestToken(uploadIds, visitorToken));
    }
    return Set.of();
  }

  private String sanitizeVisitorToken(String visitorToken) {
    if (!StringUtils.hasText(visitorToken)) {
      return null;
    }
    return visitorToken.trim();
  }

  private UserAccount resolveUser(Authentication authentication) {
    if (authentication == null || authentication instanceof AnonymousAuthenticationToken || !authentication.isAuthenticated()) {
      return null;
    }
    Object principal = authentication.getPrincipal();
    if (principal instanceof UserDetails userDetails) {
      return userAccountRepository.findByUsername(userDetails.getUsername()).orElse(null);
    }
    if (principal instanceof UserAccount userAccount) {
      return userAccount;
    }
    return null;
  }

  private PublicGalleryItemResponse mapItem(UploadRecord record, boolean likedByMe, PublicRecentLikeResponse latestLike) {
    UserAccount uploader = record.getUser();
    Long ownerId = uploader != null ? uploader.getId() : null;
    String ownerName = uploader != null ? uploader.getDisplayName() : "匿名用户";
    String ownerAvatar = uploader != null ? uploader.getAvatarUrl() : null;
    return new PublicGalleryItemResponse(
        record.getId(),
        record.getFileName(),
        record.getPublicUrl(),
        record.getObjectKey(),
        record.getSize(),
        record.getUploadedAt(),
        ownerId,
        ownerName,
        ownerAvatar,
        record.getLikeCount(),
        record.getInvokeCount(),
        likedByMe,
        latestLike
    );
  }

  private PublicRecentLikeResponse toRecentLike(UploadLike like) {
    if (like == null) {
      return null;
    }
    if (like.getUser() != null) {
      return new PublicRecentLikeResponse(
          like.getUser().getDisplayName(),
          like.getUser().getId(),
          like.getUser().getAvatarUrl(),
          false,
          like.getLikedAt()
      );
    }
    return new PublicRecentLikeResponse(
        StringUtils.hasText(like.getGuestDisplayName()) ? like.getGuestDisplayName() : "访客",
        null,
        like.getGuestAvatarUrl(),
        true,
        like.getLikedAt()
    );
  }
}

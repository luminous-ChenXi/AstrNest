package com.chenxi.astrnest.gallery;

import com.chenxi.astrnest.gallery.dto.PublicGalleryItemResponse;
import com.chenxi.astrnest.gallery.dto.PublicGalleryMetricsResponse;
import com.chenxi.astrnest.gallery.dto.PublicGalleryPageResponse;
import com.chenxi.astrnest.gallery.dto.PublicRecentLikeResponse;
import com.chenxi.astrnest.security.user.UserAccount;
import com.chenxi.astrnest.storage.PublicAssetUrlResolver;
import com.chenxi.astrnest.security.user.UserAccountRepository;
import com.chenxi.astrnest.system.SystemConfigService;
import com.chenxi.astrnest.tag.ChenxiTagService;
import com.chenxi.astrnest.tag.dto.ChenxiTagResponse;
import com.chenxi.astrnest.upload.like.UploadLike;
import com.chenxi.astrnest.upload.like.UploadLikeRepository;
import com.chenxi.astrnest.upload.media.MediaCategory;
import com.chenxi.astrnest.upload.media.VideoThumbnailService;
import com.chenxi.astrnest.upload.record.UploadRecord;
import com.chenxi.astrnest.upload.record.UploadRecordRepository;
import com.chenxi.astrnest.storage.StoredObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class PublicGalleryService {

  private final UploadRecordRepository uploadRecordRepository;
  private final UploadLikeRepository uploadLikeRepository;
  private final UserAccountRepository userAccountRepository;
  private final SystemConfigService systemConfigService;
  private final PublicAssetUrlResolver publicAssetUrlResolver;
  private final ChenxiTagService chenxiTagService;
  private final com.chenxi.astrnest.upload.media.VideoThumbnailService videoThumbnailService;

  @Transactional(readOnly = true)
  public PublicGalleryPageResponse list(int page, int size, Authentication authentication, String visitorToken) {
    try {
      int normalizedPage = Math.max(page, 0);
      int normalizedSize = Math.min(Math.max(size, 1), 24);
      Pageable pageable = PageRequest.of(normalizedPage, normalizedSize, Sort.by(Sort.Direction.DESC, "uploadedAt"));
      UserAccount viewer = resolveUser(authentication);
      Specification<UploadRecord> specification = buildPublicListSpecification(viewer);
      Page<UploadRecord> result = uploadRecordRepository.findAll(specification, pageable);
      List<UploadRecord> records = result.getContent();
      List<Long> recordIds = records.stream().map(UploadRecord::getId).toList();
      Map<Long, List<ChenxiTagResponse>> tagMap = resolveTags(recordIds);

      String sanitizedVisitorToken = sanitizeVisitorToken(visitorToken);
      Set<Long> likedIds = resolveLikedIds(recordIds, viewer, sanitizedVisitorToken);
      Map<Long, PublicRecentLikeResponse> latestLikeMap = buildLatestLikeMap(recordIds);
      boolean guestLikeEnabled = systemConfigService.isGuestLikeEnabled();

      List<PublicGalleryItemResponse> items = records.stream()
          .map(record -> mapItem(
              record,
              likedIds.contains(record.getId()),
              latestLikeMap.get(record.getId()),
              tagMap.getOrDefault(record.getId(), List.of())
          ))
          .toList();

      return new PublicGalleryPageResponse(
          items,
          result.getTotalElements(),
          result.getTotalPages(),
          result.getNumber(),
          result.getSize(),
          guestLikeEnabled
      );
    } catch (Exception exception) {
      log.error("Failed to list public gallery", exception);
      throw exception;
    }
  }

  @Transactional(readOnly = true)
  public PublicGalleryMetricsResponse metrics() {
    long totalPublicImages = uploadRecordRepository.countByPublicAccessibleTrueAndViolationFalse();
    long totalTags = chenxiTagService.countTags();
    return new PublicGalleryMetricsResponse(totalPublicImages, totalTags);
  }

  @Transactional(readOnly = true)
  public PublicGalleryPageResponse searchByTagKeyword(
      String keyword,
      int page,
      int size,
      Authentication authentication,
      String visitorToken
  ) {
    try {
      int normalizedPage = Math.max(page, 0);
      int normalizedSize = Math.min(Math.max(size, 1), 24);
      String sanitizedKeyword = keyword != null ? keyword.trim() : "";
      if (!StringUtils.hasText(sanitizedKeyword)) {
        boolean guestLikeEnabled = systemConfigService.isGuestLikeEnabled();
        return new PublicGalleryPageResponse(
            List.of(),
            0,
            0,
            normalizedPage,
            normalizedSize,
            guestLikeEnabled
        );
      }

      Pageable pageable = PageRequest.of(normalizedPage, normalizedSize, Sort.by(Sort.Direction.DESC, "uploadedAt"));
      UserAccount viewer = resolveUser(authentication);
      Specification<UploadRecord> specification = buildTagSearchSpecification(sanitizedKeyword, viewer);
      Page<UploadRecord> result = uploadRecordRepository.findAll(specification, pageable);
      List<UploadRecord> records = result.getContent();
      List<Long> recordIds = records.stream().map(UploadRecord::getId).toList();
      Map<Long, List<ChenxiTagResponse>> tagMap = resolveTags(recordIds);

      String sanitizedVisitorToken = sanitizeVisitorToken(visitorToken);
      Set<Long> likedIds = resolveLikedIds(recordIds, viewer, sanitizedVisitorToken);
      Map<Long, PublicRecentLikeResponse> latestLikeMap = buildLatestLikeMap(recordIds);
      boolean guestLikeEnabled = systemConfigService.isGuestLikeEnabled();

      List<PublicGalleryItemResponse> items = records.stream()
          .map(record -> mapItem(
              record,
              likedIds.contains(record.getId()),
              latestLikeMap.get(record.getId()),
              tagMap.getOrDefault(record.getId(), List.of())
          ))
          .toList();

      return new PublicGalleryPageResponse(
          items,
          result.getTotalElements(),
          result.getTotalPages(),
          result.getNumber(),
          result.getSize(),
          guestLikeEnabled
      );
    } catch (Exception exception) {
      log.error("Failed to search public gallery", exception);
      throw exception;
    }
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

  private PublicGalleryItemResponse mapItem(
      UploadRecord record,
      boolean likedByMe,
      PublicRecentLikeResponse latestLike,
      List<ChenxiTagResponse> tags
  ) {
    UserAccount uploader = record.getUser();
    Long ownerId = uploader != null ? uploader.getId() : null;
    String ownerName = uploader != null ? uploader.getDisplayName() : "匿名用户";
    String ownerAvatar = uploader != null ? uploader.getAvatarUrl() : null;
    return new PublicGalleryItemResponse(
        record.getId(),
        record.getFileName(),
        publicAssetUrlResolver.resolve(record),
        resolvedThumbnailUrl(record),
        record.getMediaCategory() != null ? record.getMediaCategory().name() : MediaCategory.IMAGE.name(),
        record.getObjectKey(),
        record.getSize(),
        record.getUploadedAt(),
        ownerId,
        ownerName,
        ownerAvatar,
        record.getLikeCount(),
        record.getInvokeCount(),
        record.isPublicAccessible(),
        likedByMe,
        tags,
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
    if (record.getMediaCategory() == MediaCategory.VIDEO) {
      VideoThumbnailService.ThumbnailResult generated = videoThumbnailService.generateThumbnail(toStoredObject(record));
      if (generated != null && StringUtils.hasText(generated.publicUrl())) {
        return generated.publicUrl();
      }
      return null;
    }
    return publicAssetUrlResolver.resolve(record);
  }

  private StoredObject toStoredObject(UploadRecord record) {
    return new StoredObject(
        record.getObjectKey(),
        record.getFileName(),
        record.getPublicUrl(),
        record.getSize(),
        record.getStorageFullPath(),
        record.getStorageProvider()
    );
  }

  private boolean isAbsoluteUrl(String value) {
    if (!StringUtils.hasText(value)) {
      return false;
    }
    String normalized = value.toLowerCase(Locale.ROOT);
    return normalized.startsWith("http://") || normalized.startsWith("https://");
  }

  private Specification<UploadRecord> buildTagSearchSpecification(String keyword, UserAccount viewer) {
    return (root, query, criteriaBuilder) -> {
      if (query != null) {
        query.distinct(true);
      }
      List<Predicate> predicates = new ArrayList<>();
      predicates.add(criteriaBuilder.isFalse(root.get("violation")));
      var albumJoin = root.join("album", JoinType.LEFT);
      if (viewer == null) {
        predicates.add(criteriaBuilder.isTrue(root.get("publicAccessible")));
        predicates.add(criteriaBuilder.or(
            criteriaBuilder.isNull(albumJoin.get("id")),
            criteriaBuilder.isTrue(albumJoin.get("isPublic"))
        ));
      } else {
        predicates.add(criteriaBuilder.or(
            criteriaBuilder.isTrue(root.get("publicAccessible")),
            criteriaBuilder.equal(root.get("user").get("id"), viewer.getId())
        ));
        predicates.add(criteriaBuilder.or(
            criteriaBuilder.isNull(albumJoin.get("id")),
            criteriaBuilder.isTrue(albumJoin.get("isPublic")),
            criteriaBuilder.equal(albumJoin.get("user").get("id"), viewer.getId())
        ));
      }
      var tagJoin = root.join("tags", JoinType.INNER);
      String normalizedKeyword = keyword.toLowerCase(Locale.ROOT);
      String likePattern = "%" + normalizedKeyword + "%";
      Predicate matchName = criteriaBuilder.like(criteriaBuilder.lower(tagJoin.get("name")), likePattern);
      Predicate matchSlug = criteriaBuilder.and(
          criteriaBuilder.isNotNull(tagJoin.get("slug")),
          criteriaBuilder.like(criteriaBuilder.lower(tagJoin.get("slug")), likePattern)
      );
      predicates.add(criteriaBuilder.or(matchName, matchSlug));
      return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
    };
  }

  private Specification<UploadRecord> buildPublicListSpecification(UserAccount viewer) {
    return (root, query, criteriaBuilder) -> {
      List<Predicate> predicates = new ArrayList<>();
      predicates.add(criteriaBuilder.isFalse(root.get("violation")));
      var albumJoin = root.join("album", JoinType.LEFT);
      if (viewer == null) {
        predicates.add(criteriaBuilder.isTrue(root.get("publicAccessible")));
        predicates.add(criteriaBuilder.or(
            criteriaBuilder.isNull(albumJoin.get("id")),
            criteriaBuilder.isTrue(albumJoin.get("isPublic"))
        ));
      } else {
        predicates.add(criteriaBuilder.or(
            criteriaBuilder.isTrue(root.get("publicAccessible")),
            criteriaBuilder.equal(root.get("user").get("id"), viewer.getId())
        ));
        predicates.add(criteriaBuilder.or(
            criteriaBuilder.isNull(albumJoin.get("id")),
            criteriaBuilder.isTrue(albumJoin.get("isPublic")),
            criteriaBuilder.equal(albumJoin.get("user").get("id"), viewer.getId())
        ));
      }
      return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
    };
  }
}

package com.chenxi.astrnest.album;

import com.chenxi.astrnest.album.dto.AlbumCreateRequest;
import com.chenxi.astrnest.album.dto.AlbumDetailResponse;
import com.chenxi.astrnest.album.dto.AlbumFeaturedResponse;
import com.chenxi.astrnest.album.dto.AlbumMediaResponse;
import com.chenxi.astrnest.album.dto.AlbumResponse;
import com.chenxi.astrnest.album.dto.AlbumUpdateRequest;
import com.chenxi.astrnest.security.user.UserAccount;
import com.chenxi.astrnest.security.user.UserAccountRepository;
import com.chenxi.astrnest.upload.record.UploadRecord;
import com.chenxi.astrnest.upload.record.UploadRecordRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlbumService {

  private final AlbumRepository albumRepository;
  private final AlbumMediaRepository albumMediaRepository;
  private final AlbumAccessLogRepository albumAccessLogRepository;
  private final UploadRecordRepository uploadRecordRepository;
  private final UserAccountRepository userAccountRepository;

  private boolean isAdmin(UserAccount user) {
    if (user == null || user.getId() == null) return false;
    return userAccountRepository.existsByIdAndRolesNameIgnoreCase(user.getId(), "ADMIN");
  }

  private UserAccount requireUser(UserAccount user) {
    if (user == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录");
    }
    return user;
  }

  @Transactional
  public AlbumResponse createAlbum(UserAccount user, AlbumCreateRequest request) {
    UserAccount currentUser = requireUser(user);

    if (albumRepository.existsByPathSlug(request.getPathSlug())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "路径标识已被使用");
    }

    Album album = new Album();
    album.setUser(currentUser);
    album.setPathSlug(request.getPathSlug());
    album.setName(request.getName());
    album.setDescription(request.getDescription());
    album.setPublic(request.getIsPublic() != null ? request.getIsPublic() : false);

    Album saved = albumRepository.save(album);
    return convertToResponse(saved);
  }

  @Transactional
  public AlbumResponse updateAlbum(UserAccount user, String albumUuid, AlbumUpdateRequest request) {
    UserAccount currentUser = requireUser(user);
    Album album = albumRepository.findByAlbumUuid(albumUuid)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "图集不存在"));

    if (!album.getUser().getId().equals(currentUser.getId()) && !isAdmin(currentUser)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权修改此图集");
    }

    if (request.getPathSlug() != null && !request.getPathSlug().equals(album.getPathSlug())) {
      if (albumRepository.existsByPathSlugAndIdNot(request.getPathSlug(), album.getId())) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, "路径标识已被使用");
      }
      album.setPathSlug(request.getPathSlug());
    }

    if (request.getName() != null) {
      album.setName(request.getName());
    }
    if (request.getDescription() != null) {
      album.setDescription(request.getDescription());
    }
    if (request.getIsPublic() != null) {
      album.setPublic(request.getIsPublic());
    }
    if (request.getCoverImageUuid() != null) {
      album.setCoverImageUuid(request.getCoverImageUuid());
    }

    Album saved = albumRepository.save(album);
    return convertToResponse(saved);
  }

  @Transactional
  public void deleteAlbum(UserAccount user, String albumUuid) {
    UserAccount currentUser = requireUser(user);
    Album album = albumRepository.findByAlbumUuid(albumUuid)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "图集不存在"));

    if (!album.getUser().getId().equals(currentUser.getId()) && !isAdmin(currentUser)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权删除此图集");
    }

    albumRepository.delete(album);
  }

  @Transactional
  public AlbumDetailResponse getAlbumDetail(UserAccount user, String albumUuid) {
    Album album = albumRepository.findByAlbumUuid(albumUuid)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "图集不存在"));

    if (!album.isPublic() && (user == null || (!album.getUser().getId().equals(user.getId()) && !isAdmin(user)))) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权访问此图集");
    }

    List<AlbumMedia> albumMedias = albumMediaRepository.findByAlbumIdOrderBySortOrderAsc(album.getId());
    List<AlbumMediaResponse> mediaResponses = new ArrayList<>();
    for (AlbumMedia media : albumMedias) {
      uploadRecordRepository.findByMediaUuid(media.getMediaUuid())
          .ifPresent(record -> mediaResponses.add(convertToMediaResponse(media, record)));
    }

    AlbumDetailResponse response = new AlbumDetailResponse();
    response.setAlbum(convertToResponse(album));
    response.setMedias(mediaResponses);
    response.setTotalMedia((long) mediaResponses.size());

    return response;
  }

  public Page<AlbumResponse> listUserAlbums(UserAccount user, Pageable pageable) {
    UserAccount currentUser = requireUser(user);
    Page<Album> albums = albumRepository.findByUserId(currentUser.getId(), pageable);
    return albums.map(this::convertToResponse);
  }

  public List<AlbumResponse> listUserPublicAlbums(Long userId) {
    return albumRepository.findByUserIdAndIsPublicTrue(userId).stream()
        .map(this::convertToResponse)
        .collect(Collectors.toList());
  }

  @Transactional
  public void addMediaToAlbum(UserAccount user, String albumUuid, String mediaUuid) {
    UserAccount currentUser = requireUser(user);
    Album album = albumRepository.findByAlbumUuid(albumUuid)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "图集不存在"));

    if (!album.getUser().getId().equals(currentUser.getId()) && !isAdmin(currentUser)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权修改此图集");
    }

    if (albumMediaRepository.existsByAlbumIdAndMediaUuid(album.getId(), mediaUuid)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "图片已在图集中");
    }

    UploadRecord uploadRecord = uploadRecordRepository.findByMediaUuid(mediaUuid)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "图片不存在"));
    if (!uploadRecord.getUser().getId().equals(currentUser.getId()) && !isAdmin(currentUser)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权操作此图片");
    }

    AlbumMedia albumMedia = new AlbumMedia();
    albumMedia.setAlbum(album);
    albumMedia.setMediaUuid(mediaUuid);
    albumMedia.setAddedBy(currentUser);

    long count = albumMediaRepository.countByAlbumId(album.getId());
    albumMedia.setSortOrder((int) count);

    albumMediaRepository.save(albumMedia);

    uploadRecord.setAlbum(album);
    uploadRecordRepository.save(uploadRecord);

    if (album.getCoverImageUuid() == null) {
      album.setCoverImageUuid(mediaUuid);
      albumRepository.save(album);
    }
  }

  @Transactional
  public void removeMediaFromAlbum(UserAccount user, String albumUuid, String mediaUuid) {
    UserAccount currentUser = requireUser(user);
    Album album = albumRepository.findByAlbumUuid(albumUuid)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "图集不存在"));

    if (!album.getUser().getId().equals(currentUser.getId()) && !isAdmin(currentUser)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权修改此图集");
    }

    albumMediaRepository.deleteByAlbumIdAndMediaUuid(album.getId(), mediaUuid);

    UploadRecord uploadRecord = uploadRecordRepository.findByMediaUuid(mediaUuid).orElse(null);
    if (uploadRecord != null && album.equals(uploadRecord.getAlbum())) {
      uploadRecord.setAlbum(null);
      uploadRecordRepository.save(uploadRecord);
    }

    if (mediaUuid.equals(album.getCoverImageUuid())) {
      List<AlbumMedia> remaining = albumMediaRepository.findByAlbumIdOrderBySortOrderAsc(album.getId());
      if (!remaining.isEmpty()) {
        album.setCoverImageUuid(remaining.get(0).getMediaUuid());
      } else {
        album.setCoverImageUuid(null);
      }
      albumRepository.save(album);
    }
  }

  @Transactional
  public ResponseEntity<Void> serveRandomImage(String pathSlug, HttpServletRequest request) {
    Album album = albumRepository.findByPathSlug(pathSlug)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "图集不存在"));

    if (!album.isPublic()) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "图集未公开");
    }

    List<AlbumMedia> medias = albumMediaRepository.findByAlbumIdOrderBySortOrderAsc(album.getId());
    List<AlbumMedia> visibleMedias = new ArrayList<>();
    for (AlbumMedia media : medias) {
      uploadRecordRepository.findByMediaUuid(media.getMediaUuid())
          .filter(this::isPublicVisible)
          .ifPresent(record -> visibleMedias.add(media));
    }

    if (visibleMedias.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "图集中暂无公开图片");
    }

    AlbumMedia randomMedia = pickRandomMedia(visibleMedias)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "无法获取图片"));

    UploadRecord uploadRecord = uploadRecordRepository.findByMediaUuid(randomMedia.getMediaUuid())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "图片不存在"));

    albumRepository.incrementAccessCount(album.getId());

    logAccess(album, randomMedia.getMediaUuid(), request);

    java.net.URI redirectUri = java.net.URI.create(uploadRecord.getPublicUrl());
    return ResponseEntity.status(HttpStatus.FOUND)
        .location(redirectUri)
        .header("Cache-Control", "public, max-age=300") //$NON-NLS-1$
        .build();
  }

  private Optional<AlbumMedia> pickRandomMedia(List<AlbumMedia> visibleMedias) {
    try {
      int offset = ThreadLocalRandom.current().nextInt(visibleMedias.size());
      return Optional.of(visibleMedias.get(offset));
    } catch (Exception e) {
      log.warn("Random pick failed: {}", e.getMessage());
      return visibleMedias.isEmpty() ? Optional.empty() : Optional.of(visibleMedias.get(0));
    }
  }

  private void logAccess(Album album, String mediaUuid, HttpServletRequest request) {
    try {
      AlbumAccessLog log = new AlbumAccessLog();
      log.setAlbum(album);
      log.setMediaUuid(mediaUuid);
      log.setClientIp(getClientIp(request));
      log.setUserAgent(request.getHeader("User-Agent"));
      log.setReferer(request.getHeader("Referer"));
      albumAccessLogRepository.save(log);
    } catch (Exception e) {
      log.warn("Failed to log album access: {}", e.getMessage());
    }
  }

  private String getClientIp(HttpServletRequest request) {
    String xForwardedFor = request.getHeader("X-Forwarded-For");
    if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
      return xForwardedFor.split(",")[0].trim();
    }
    String xRealIp = request.getHeader("X-Real-IP");
    if (xRealIp != null && !xRealIp.isEmpty()) {
      return xRealIp;
    }
    return request.getRemoteAddr();
  }

  private AlbumResponse convertToResponse(Album album) {
    try {
      AlbumResponse response = new AlbumResponse();
      response.setIsPublic(album.isPublic());
      response.setId(album.getId());
      response.setAlbumUuid(album.getAlbumUuid());
      response.setPathSlug(album.getPathSlug());
      response.setName(album.getName());
      response.setDescription(album.getDescription());
      response.setIsPublic(album.isPublic());
      response.setCoverImageUuid(album.getCoverImageUuid());
      response.setAccessCount(album.getAccessCount());
      response.setCreatedAt(album.getCreatedAt());
      response.setUpdatedAt(album.getUpdatedAt());

      try {
        long mediaCount = albumMediaRepository.countByAlbumId(album.getId());
        response.setMediaCount(mediaCount);
      } catch (Exception e) {
        log.warn("Failed to count media for album {}: {}", album.getId(), e.getMessage());
        response.setMediaCount(0L);
      }

      if (album.getUser() != null) {
        try {
          response.setUserId(album.getUser().getId());
          response.setUsername(album.getUser().getUsername());
        } catch (Exception e) {
          log.warn("Failed to get user info for album {}: {}", album.getId(), e.getMessage());
        }
      }

      // 获取预览图片列表（最多3张，用于卡片轮播）
      try {
        List<AlbumMedia> previewMedias = albumMediaRepository.findTop3ByAlbumIdOrderBySortOrderAsc(album.getId());
        List<String> previewUuids = previewMedias.stream()
            .map(AlbumMedia::getMediaUuid)
            .collect(Collectors.toList());
        response.setPreviewImageUuids(previewUuids);
      } catch (Exception e) {
        log.warn("Failed to get preview images for album {}: {}", album.getId(), e.getMessage());
        response.setPreviewImageUuids(new ArrayList<>());
      }

      return response;
    } catch (Exception e) {
      log.error("Error converting album to response: {}", e.getMessage(), e);
      throw e;
    }
  }

  private AlbumMediaResponse convertToMediaResponse(AlbumMedia albumMedia, UploadRecord record) {
    AlbumMediaResponse response = new AlbumMediaResponse();
    response.setId(albumMedia.getId());
    response.setMediaUuid(albumMedia.getMediaUuid());
    response.setAddedAt(albumMedia.getAddedAt());
    response.setSortOrder(albumMedia.getSortOrder());

    response.setFileName(record.getFileName());
    response.setPublicUrl(record.getPublicUrl());
    response.setThumbnailUrl(record.getThumbnailUrl());
    response.setContentType(record.getContentType());
    response.setSize(record.getSize());
    response.setWidth(record.getWidth());
    response.setHeight(record.getHeight());

    return response;
  }

  private boolean isPublicVisible(UploadRecord record) {
    return record != null && record.isPublicAccessible() && !record.isViolation();
  }

  /**
   * 获取用户可添加到图集的图片列表（排除已在图集中的图片）
   */
  @Transactional(readOnly = true)
  public Page<com.chenxi.astrnest.album.dto.AvailableMediaResponse> getAvailableMediasForAlbum(UserAccount user, String albumUuid, Pageable pageable) {
    UserAccount currentUser = requireUser(user);
    Album album = albumRepository.findByAlbumUuid(albumUuid)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "图集不存在"));

    // 只有图集创建者才能添加图片
    if (!album.getUser().getId().equals(currentUser.getId())) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "只有图集创建者才能添加图片");
    }

    Page<UploadRecord> records = uploadRecordRepository.findByUserIdAndNotInAlbum(currentUser.getId(), album.getId(), pageable);
    return records.map(this::convertToAvailableMediaResponse);
  }

  private com.chenxi.astrnest.album.dto.AvailableMediaResponse convertToAvailableMediaResponse(UploadRecord record) {
    return com.chenxi.astrnest.album.dto.AvailableMediaResponse.builder()
        .id(record.getId())
        .mediaUuid(record.getMediaUuid())
        .fileName(record.getFileName())
        .publicUrl(record.getPublicUrl())
        .thumbnailUrl(record.getThumbnailUrl())
        .contentType(record.getContentType())
        .size(record.getSize())
        .width(record.getWidth())
        .height(record.getHeight())
        .uploadedAt(record.getUploadedAt())
        .publicAccessible(record.isPublicAccessible())
        .violation(record.isViolation())
        .build();
  }

  /**
   * 获取公开图集详情（若用户为拥有者或管理员，可访问私密图集）
   */
  @Transactional(readOnly = true)
  public AlbumResponse getPublicAlbumDetail(String pathSlug, UserAccount viewer) {
    Album album = albumRepository.findByPathSlug(pathSlug)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "图集不存在"));

    boolean isOwner = viewer != null && album.getUser().getId().equals(viewer.getId());
    boolean isAdmin = isAdmin(viewer);
    boolean isOwnerOrAdmin = isOwner || isAdmin;

    if (!album.isPublic() && !isOwnerOrAdmin) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "此图集为私有图集或没有权限，无法访问");
    }

    AlbumResponse response = convertToResponse(album);

    // 设置权限信息
    response.setIsOwner(isOwner);
    response.setIsAdmin(isAdmin);
    response.setCanEdit(isOwnerOrAdmin);
    response.setCanAddMedia(isOwner);

    long publicMediaCount = isOwnerOrAdmin
        ? albumMediaRepository.countByAlbumId(album.getId())
        : uploadRecordRepository.countByAlbumIdAndPublicAccessibleTrueAndViolationFalse(album.getId());
    response.setMediaCount(publicMediaCount);

    if (album.getCoverImageUuid() != null) {
      Optional<UploadRecord> coverRecord = uploadRecordRepository.findByMediaUuid(album.getCoverImageUuid());
      if (coverRecord.isEmpty() || (!isOwnerOrAdmin && !isPublicVisible(coverRecord.get()))) {
        // 封面不可见时回退到首个可见图片
        Optional<UploadRecord> firstVisible = uploadRecordRepository
            .findByAlbumIdAndPublicAccessibleTrueAndViolationFalseOrderByUploadedAtDesc(album.getId())
            .stream()
            .findFirst();
        response.setCoverImageUuid(firstVisible.map(UploadRecord::getMediaUuid).orElse(null));
      }
    }

    return response;
  }

  /**
   * 获取图集中的所有图片（公开用户只见公开，拥有者/管理员可见全部）
   */
  @Transactional(readOnly = true)
  public List<AlbumMediaResponse> getPublicAlbumMedias(String pathSlug, UserAccount viewer) {
    Album album = albumRepository.findByPathSlug(pathSlug)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "图集不存在"));

    boolean isOwnerOrAdmin = viewer != null && (isAdmin(viewer) || album.getUser().getId().equals(viewer.getId()));
    if (!album.isPublic() && !isOwnerOrAdmin) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "图集未公开");
    }

    List<AlbumMedia> medias = albumMediaRepository.findByAlbumIdOrderBySortOrderAsc(album.getId());

    List<AlbumMediaResponse> responses = new ArrayList<>();
    for (AlbumMedia media : medias) {
      uploadRecordRepository.findByMediaUuid(media.getMediaUuid())
          .filter(record -> isOwnerOrAdmin || isPublicVisible(record))
          .ifPresent(record -> responses.add(convertToMediaResponse(media, record)));
    }

    return responses;
  }

  /**
   * 获取首页Featured图集（最受欢迎的公开图集）
   * 按图集内所有图片的喜欢数总和排序，取前3个
   */
  @Transactional(readOnly = true)
  public List<AlbumFeaturedResponse> getFeaturedAlbums() {
    List<Album> publicAlbums = albumRepository.findByIsPublicTrue();

    List<AlbumFeaturedResponse> featuredList = new ArrayList<>();

    for (Album album : publicAlbums) {
      List<AlbumMedia> medias = albumMediaRepository.findByAlbumId(album.getId());

      // 计算图集内所有图片的喜欢数总和
      long totalLikes = 0;
      for (AlbumMedia media : medias) {
        Optional<UploadRecord> recordOpt = uploadRecordRepository.findByMediaUuid(media.getMediaUuid());
        if (recordOpt.isPresent()) {
          UploadRecord record = recordOpt.get();
          // 只统计公开可见且未违规的图片
          if (record.isPublicAccessible() && !record.isViolation()) {
            totalLikes += record.getLikeCount();
          }
        }
      }

      // 只返回有图片的图集
      if (!medias.isEmpty()) {
        AlbumFeaturedResponse response = new AlbumFeaturedResponse();
        response.setId(album.getId());
        response.setAlbumUuid(album.getAlbumUuid());
        response.setPathSlug(album.getPathSlug());
        response.setName(album.getName());
        response.setDescription(album.getDescription());
        response.setCoverImageUuid(album.getCoverImageUuid());
        response.setMediaCount((long) medias.size());
        response.setTotalLikes(totalLikes);
        response.setUsername(album.getUser() != null ? album.getUser().getUsername() : null);
        response.setCreatedAt(album.getCreatedAt());

        featuredList.add(response);
      }
    }

    // 按喜欢数总和降序排序，取前3个
    return featuredList.stream()
        .sorted((a, b) -> Long.compare(b.getTotalLikes(), a.getTotalLikes()))
        .limit(3)
        .collect(Collectors.toList());
  }
}

package com.chenxi.astrnest.album;

import com.chenxi.astrnest.album.dto.AlbumCreateRequest;
import com.chenxi.astrnest.album.dto.AlbumDetailResponse;
import com.chenxi.astrnest.album.dto.AlbumMediaResponse;
import com.chenxi.astrnest.album.dto.AlbumResponse;
import com.chenxi.astrnest.album.dto.AlbumUpdateRequest;
import com.chenxi.astrnest.security.user.UserAccount;
import com.chenxi.astrnest.upload.record.UploadRecord;
import com.chenxi.astrnest.upload.record.UploadRecordRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlbumService {

  private final AlbumRepository albumRepository;
  private final AlbumMediaRepository albumMediaRepository;
  private final AlbumAccessLogRepository albumAccessLogRepository;
  private final UploadRecordRepository uploadRecordRepository;

  private boolean isAdmin(UserAccount user) {
    if (user == null) return false;
    return user.getRoles().stream()
        .anyMatch(role -> "admin".equalsIgnoreCase(role.getName()));
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
    List<AlbumMediaResponse> mediaResponses = albumMedias.stream()
        .map(this::convertToMediaResponse)
        .collect(Collectors.toList());

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

    long mediaCount = albumMediaRepository.countByAlbumId(album.getId());
    if (mediaCount == 0) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "图集为空");
    }

    AlbumMedia randomMedia = pickRandomMedia(album.getId(), mediaCount)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "无法获取图片"));

    UploadRecord uploadRecord = uploadRecordRepository.findByMediaUuid(randomMedia.getMediaUuid())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "图片不存在"));

    albumRepository.incrementAccessCount(album.getId());

    logAccess(album, randomMedia.getMediaUuid(), request);

    return ResponseEntity.status(HttpStatus.FOUND)
        .location(URI.create(uploadRecord.getPublicUrl()))
        .header("Cache-Control", "public, max-age=300")
        .build();
  }

  private Optional<AlbumMedia> pickRandomMedia(Long albumId, long mediaCount) {
    try {
      int offset = ThreadLocalRandom.current().nextInt((int) mediaCount);
      Page<AlbumMedia> page = albumMediaRepository.findByAlbumIdOrderByIdAsc(
          albumId, PageRequest.of(offset, 1));
      if (page.hasContent()) {
        return Optional.of(page.getContent().getFirst());
      }
    } catch (Exception e) {
      log.warn("Random pick via offset failed, fallback to RAND(): {}", e.getMessage());
    }
    return albumMediaRepository.findRandomByAlbumId(albumId);
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

      return response;
    } catch (Exception e) {
      log.error("Error converting album to response: {}", e.getMessage(), e);
      throw e;
    }
  }

  private AlbumMediaResponse convertToMediaResponse(AlbumMedia albumMedia) {
    AlbumMediaResponse response = new AlbumMediaResponse();
    response.setId(albumMedia.getId());
    response.setMediaUuid(albumMedia.getMediaUuid());
    response.setAddedAt(albumMedia.getAddedAt());
    response.setSortOrder(albumMedia.getSortOrder());

    Optional<UploadRecord> uploadRecord = uploadRecordRepository.findByMediaUuid(albumMedia.getMediaUuid());
    if (uploadRecord.isPresent()) {
      UploadRecord record = uploadRecord.get();
      response.setFileName(record.getFileName());
      response.setPublicUrl(record.getPublicUrl());
      response.setContentType(record.getContentType());
      response.setSize(record.getSize());
    }

    return response;
  }
}

package com.chenxi.astrnest.gallery;

import com.chenxi.astrnest.gallery.dto.PublicRecentLikeResponse;
import com.chenxi.astrnest.gallery.dto.PublicToggleLikeResponse;
import com.chenxi.astrnest.security.user.UserAccount;
import com.chenxi.astrnest.security.user.UserAccountRepository;
import com.chenxi.astrnest.system.SystemConfigService;
import com.chenxi.astrnest.upload.like.UploadLike;
import com.chenxi.astrnest.upload.like.UploadLikeRepository;
import com.chenxi.astrnest.upload.record.UploadRecord;
import com.chenxi.astrnest.upload.record.UploadRecordRepository;
import jakarta.transaction.Transactional;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class PublicGalleryLikeService {

  private static final String DEFAULT_GUEST_NAME = "访客";

  private final UploadRecordRepository uploadRecordRepository;
  private final UploadLikeRepository uploadLikeRepository;
  private final UserAccountRepository userAccountRepository;
  private final SystemConfigService systemConfigService;

  @Transactional
  public PublicToggleLikeResponse like(Long uploadId, Authentication authentication, String visitorToken,
      String userAgent, String requesterIp) {
    UploadRecord record = requirePublicRecord(uploadId);
    Identity identity = resolveIdentity(authentication, visitorToken, userAgent, requesterIp);

    if (identity.user() != null) {
      if (!uploadLikeRepository.existsByUploadRecordIdAndUserId(record.getId(), identity.user().getId())) {
        persistUserLike(record, identity.user());
      }
      return buildResponse(record, true);
    }

    ensureGuestLikeAllowed();
    if (!StringUtils.hasText(identity.guestToken())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "访客标识缺失，请刷新后重新点赞");
    }
    if (!uploadLikeRepository.existsByUploadRecordIdAndGuestToken(record.getId(), identity.guestToken())) {
      persistGuestLike(record, identity.guestToken(), identity.guestDisplayName());
    }
    return buildResponse(record, true);
  }

  @Transactional
  public PublicToggleLikeResponse unlike(Long uploadId, Authentication authentication, String visitorToken,
      String userAgent, String requesterIp) {
    UploadRecord record = requirePublicRecord(uploadId);
    Identity identity = resolveIdentity(authentication, visitorToken, userAgent, requesterIp);

    if (identity.user() != null) {
      uploadLikeRepository.findByUploadRecordIdAndUserId(record.getId(), identity.user().getId())
          .ifPresent(uploadLikeRepository::delete);
      return buildResponse(record, false);
    }

    if (!StringUtils.hasText(identity.guestToken())) {
      return buildResponse(record, false);
    }
    uploadLikeRepository.findByUploadRecordIdAndGuestToken(record.getId(), identity.guestToken())
        .ifPresent(uploadLikeRepository::delete);
    return buildResponse(record, false);
  }

  private void persistUserLike(UploadRecord record, UserAccount user) {
    UploadLike like = new UploadLike();
    like.setUploadRecord(record);
    like.setUser(user);
    like.setLikedAsGuest(false);
    uploadLikeRepository.save(like);
  }

  private void persistGuestLike(UploadRecord record, String guestToken, String guestDisplayName) {
    UploadLike like = new UploadLike();
    like.setUploadRecord(record);
    like.setGuestToken(guestToken);
    like.setGuestDisplayName(StringUtils.hasText(guestDisplayName) ? guestDisplayName : DEFAULT_GUEST_NAME);
    like.setLikedAsGuest(true);
    uploadLikeRepository.save(like);
  }

  private PublicToggleLikeResponse buildResponse(UploadRecord record, boolean liked) {
    long likeCount = uploadLikeRepository.countByUploadRecordId(record.getId());
    record.setLikeCount(likeCount);
    uploadRecordRepository.save(record);
    Optional<UploadLike> latest = uploadLikeRepository.findFirstByUploadRecordIdOrderByLikedAtDesc(record.getId());
    return new PublicToggleLikeResponse(likeCount, liked, latest.map(this::toRecentLike).orElse(null));
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
        StringUtils.hasText(like.getGuestDisplayName()) ? like.getGuestDisplayName() : DEFAULT_GUEST_NAME,
        null,
        like.getGuestAvatarUrl(),
        true,
        like.getLikedAt()
    );
  }

  private UploadRecord requirePublicRecord(Long uploadId) {
    return uploadRecordRepository.findByIdAndPublicAccessibleTrueAndViolationFalse(uploadId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "图片不存在或不可公开点赞"));
  }

  private Identity resolveIdentity(Authentication authentication, String visitorToken, String userAgent, String requesterIp) {
    UserAccount user = resolveUser(authentication);
    if (user != null) {
      return Identity.user(user);
    }
    String derivedToken = deriveVisitorToken(visitorToken, requesterIp, userAgent);
    return Identity.guest(derivedToken, DEFAULT_GUEST_NAME);
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

  @SuppressWarnings("null")
  private String deriveVisitorToken(String explicitToken, String requesterIp, String userAgent) {
    if (StringUtils.hasText(explicitToken)) {
      return explicitToken.trim();
    }
    String fingerprint = (StringUtils.hasText(requesterIp) ? requesterIp : "127.0.0.1") + "|" + (userAgent == null ? "ua" : userAgent);
    byte[] fingerprintBytes = fingerprint.getBytes(StandardCharsets.UTF_8);
    return DigestUtils.md5DigestAsHex(fingerprintBytes);
  }

  private void ensureGuestLikeAllowed() {
    if (!systemConfigService.isGuestLikeEnabled()) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "已关闭 访客点赞，可登录后操作");
    }
  }

  private record Identity(UserAccount user, String guestToken, String guestDisplayName) {

    static Identity user(UserAccount user) {
      return new Identity(user, null, null);
    }

    static Identity guest(String guestToken, String guestDisplayName) {
      return new Identity(null, guestToken, guestDisplayName);
    }
  }
}

package com.chenxi.astrnest.upload.record;

import com.chenxi.astrnest.ai.AiDecision;
import com.chenxi.astrnest.ai.TencentAiError;
import com.chenxi.astrnest.security.apikey.ApiKey;
import com.chenxi.astrnest.security.user.UserAccount;
import com.chenxi.astrnest.storage.StorageContext;
import com.chenxi.astrnest.storage.StorageService;
import com.chenxi.astrnest.storage.StoredObject;
import com.chenxi.astrnest.tag.ChenxiTag;
import com.chenxi.astrnest.upload.like.UploadLike;
import com.chenxi.astrnest.upload.like.UploadLikeRepository;
import com.chenxi.astrnest.upload.media.MediaCategory;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class UploadRecordService {

  private final UploadRecordRepository uploadRecordRepository;
  private final UploadLikeRepository uploadLikeRepository;
  private final StorageService storageService;

  @Transactional
  public UploadRecord recordUserUpload(UserAccount user, ApiKey apiKey, StoredObject storedObject,
      String publicUrl, String contentType, String reviewStatus, StorageContext storageContext,
      String uploaderIp, Collection<ChenxiTag> tags, MediaCategory mediaCategory,
      String thumbnailUrl, String thumbnailStoragePath, Integer durationSeconds, String embedUrl, String mediaUuid,
      AiDecision aiDecision, String aiLabelSnapshot, TencentAiError aiError,
      Integer width, Integer height) {
    UploadRecord record = new UploadRecord();
    record.setMediaUuid(StringUtils.hasText(mediaUuid) ? mediaUuid : UUID.randomUUID().toString());
    record.setUser(user);
    record.setApiKey(apiKey);
    record.setObjectKey(storedObject.objectKey());
    record.setPublicUrl(StringUtils.hasText(publicUrl) ? publicUrl : storedObject.publicUrl());
    record.setFileName(storedObject.storedFileName());
    record.setContentType(contentType);
    record.setMediaCategory(mediaCategory == null ? MediaCategory.IMAGE : mediaCategory);
    record.setSize(storedObject.size());
    record.setReviewStatus(reviewStatus);
    record.setStorageProvider(storedObject.providerKey() != null ? storedObject.providerKey() : resolveStorageProvider(storageContext));
    record.setStorageMode(storageContext != null ? storageContext.visibility() : "PUBLIC");
    boolean violation = isViolation(reviewStatus);
    record.setViolation(violation);
    // 默认私密，违规内容也保持私密
    record.setPublicAccessible(false);
    record.setLikeCount(0L);
    record.setInvokeCount(0L);
    record.setUploaderIp(uploaderIp);
    record.setStorageFullPath(storedObject.absolutePath());
    record.setThumbnailUrl(thumbnailUrl);
    record.setThumbnailStoragePath(thumbnailStoragePath);
    record.setDurationSeconds(durationSeconds);
    record.setEmbedUrl(embedUrl);
    record.setWidth(width);
    record.setHeight(height);
    record.setAiDecision(aiDecision);
    record.setAiLabelSnapshot(aiLabelSnapshot);
    if (aiError != null) {
      record.setAiErrorCode(aiError.code());
      String friendly = StringUtils.hasText(aiError.friendlyMessage())
          ? aiError.friendlyMessage()
          : aiError.message();
      record.setAiErrorMessage(friendly);
      record.setAiRequestId(aiError.requestId());
    } else {
      record.setAiErrorCode(null);
      record.setAiErrorMessage(null);
      record.setAiRequestId(null);
    }
    if (tags != null && !tags.isEmpty()) {
      record.getTags().clear();
      record.getTags().addAll(tags);
    }
    return uploadRecordRepository.save(record);
  }

  @Transactional(readOnly = true)
  public UploadRecord requireOwnedRecord(Long recordId, Long userId) {
    return uploadRecordRepository.findByIdAndUserId(recordId, userId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "图片不存在或无权访问"));
  }

  @Transactional
  public UploadRecord updateVisibility(UploadRecord record, boolean publicAccessible) {
    record.setPublicAccessible(publicAccessible);
    return uploadRecordRepository.save(record);
  }

  @Transactional
  public UploadRecord markViolation(UploadRecord record, boolean violation, String placeholderUrl,
      boolean deletePhysicalFile, String reviewStatus) {
    if (record == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "图片记录不存在");
    }
    record.setViolation(violation);
    record.setPublicAccessible(!violation);
    if (StringUtils.hasText(reviewStatus)) {
      record.setReviewStatus(reviewStatus);
    }
    if (violation && StringUtils.hasText(placeholderUrl)) {
      record.setPublicUrl(placeholderUrl);
      record.setThumbnailUrl(placeholderUrl);
    }
    if (violation && deletePhysicalFile && StringUtils.hasText(record.getObjectKey())) {
      deleteStoredFile(record.getObjectKey(), record.getStorageProvider());
    }
    String finalStatus = StringUtils.hasText(record.getReviewStatus()) ? record.getReviewStatus() : "UNSPECIFIED";
    log.info("上传记录 {} 审核状态变更为 {}，违规标识 {}", record.getId(), finalStatus, violation);
    return uploadRecordRepository.save(record);
  }

  @Transactional
  public LikeMutation toggleLike(UploadRecord record, UserAccount user) {
    boolean liked;
    if (uploadLikeRepository.existsByUploadRecordIdAndUserId(record.getId(), user.getId())) {
      uploadLikeRepository.findByUploadRecordIdAndUserId(record.getId(), user.getId())
          .ifPresent(uploadLikeRepository::delete);
      liked = false;
    } else {
      UploadLike like = new UploadLike();
      like.setUploadRecord(record);
      like.setUser(user);
      uploadLikeRepository.save(like);
      liked = true;
    }
    long likeCount = uploadLikeRepository.countByUploadRecordId(record.getId());
    record.setLikeCount(likeCount);
    uploadRecordRepository.save(record);
    return new LikeMutation(likeCount, liked);
  }

  public boolean isLikedBy(Long recordId, Long userId) {
    return uploadLikeRepository.existsByUploadRecordIdAndUserId(recordId, userId);
  }

  public Page<UploadRecord> findForUser(Long userId, Pageable pageable) {
    return uploadRecordRepository.findByUserIdOrderByUploadedAtDesc(userId, pageable);
  }

  public long countTotalForUser(Long userId) {
    return uploadRecordRepository.countByUserId(userId);
  }

  public long countTodayForUser(Long userId) {
    Instant startOfDay = LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC);
    return uploadRecordRepository.countByUserIdAndUploadedAtAfter(userId, startOfDay);
  }

  public long totalSizeForUser(Long userId) {
    return uploadRecordRepository.totalSizeByUser(userId);
  }

  @Transactional
  public void recordFetch(String objectKey) {
        uploadRecordRepository.findByObjectKey(objectKey)
        .ifPresent(record -> {
          record.setInvokeCount(record.getInvokeCount() + 1);
          record.setLastAccessAt(Instant.now());
          uploadRecordRepository.save(record);
        });
  }

  @Transactional
  public void deleteUserRecord(Long recordId, Long userId) {
    uploadRecordRepository.findByIdAndUserId(recordId, userId)
        .ifPresent(record -> deleteRecord(record));
  }

  @Transactional
  public void deleteRecord(UploadRecord record) {
    deleteStoredFile(record.getObjectKey(), record.getStorageProvider());
    uploadRecordRepository.delete(record);
  }

  @Transactional
  public int deleteExpiredRecords(Instant threshold) {
    List<UploadRecord> expiredRecords = uploadRecordRepository.findExpiredSince(threshold);
    int removed = 0;
    for (UploadRecord record : expiredRecords) {
      deleteRecord(record);
      removed++;
    }
    return removed;
  }


  private void deleteStoredFile(String objectKey, String providerKey) {
    try {
      storageService.delete(objectKey, providerKey);
    } catch (Exception exception) {
      log.warn("删除存储文件失败 {}：{}", objectKey, exception.getMessage());
    }
  }

  private String resolveStorageProvider(StorageContext storageContext) {
    if (storageContext == null || storageContext.providerKey() == null) {
      return "LOCAL_DISK";
    }
    return storageContext.providerKey();
  }

  private boolean isViolation(String status) {
    if (status == null) {
      return false;
    }
    String normalized = status.trim().toLowerCase();
    return normalized.contains("violation") || normalized.contains("block") || normalized.contains("reject");
  }

  public record LikeMutation(long likeCount, boolean liked) {}
}

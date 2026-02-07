package com.imgbed.upload.record;

import com.imgbed.security.apikey.ApiKey;
import com.imgbed.security.user.UserAccount;
import com.imgbed.storage.StorageContext;
import com.imgbed.storage.StoredObject;
import com.imgbed.upload.like.UploadLike;
import com.imgbed.upload.like.UploadLikeRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class UploadRecordService {

  private final UploadRecordRepository uploadRecordRepository;
  private final UploadLikeRepository uploadLikeRepository;

  @Transactional
  public UploadRecord recordUserUpload(UserAccount user, ApiKey apiKey, StoredObject storedObject,
      String contentType, String reviewStatus, StorageContext storageContext, String uploaderIp) {
    UploadRecord record = new UploadRecord();
    record.setUser(user);
    record.setApiKey(apiKey);
    record.setObjectKey(storedObject.objectKey());
    record.setPublicUrl(storedObject.publicUrl());
    record.setFileName(storedObject.storedFileName());
    record.setContentType(contentType);
    record.setSize(storedObject.size());
    record.setReviewStatus(reviewStatus);
    record.setStorageProvider(storedObject.providerKey() != null ? storedObject.providerKey() : resolveStorageProvider(storageContext));
    record.setStorageMode(storageContext != null ? storageContext.visibility() : "PUBLIC");
    record.setViolation(isViolation(reviewStatus));
    record.setPublicAccessible(true);
    record.setLikeCount(0L);
    record.setInvokeCount(0L);
    record.setUploaderIp(uploaderIp);
    record.setStorageFullPath(storedObject.absolutePath());
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
          uploadRecordRepository.save(record);
        });
  }

  @Transactional
  public void deleteUserRecord(Long recordId, Long userId) {
    uploadRecordRepository.findByIdAndUserId(recordId, userId)
        .ifPresent(uploadRecordRepository::delete);
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

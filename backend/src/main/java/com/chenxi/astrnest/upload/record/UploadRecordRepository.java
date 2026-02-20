package com.chenxi.astrnest.upload.record;

import com.chenxi.astrnest.security.apikey.dto.ApiKeyUsageAggregate;
import com.chenxi.astrnest.upload.record.dto.UserUsageAggregate;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UploadRecordRepository extends JpaRepository<UploadRecord, Long>, JpaSpecificationExecutor<UploadRecord> {

  Page<UploadRecord> findByUserIdOrderByUploadedAtDesc(Long userId, Pageable pageable);

  long countByUserId(Long userId);

  long countByUserIdAndUploadedAtAfter(Long userId, Instant after);

  @Query("select coalesce(sum(r.size),0) from UploadRecord r where r.user.id = :userId")
  long totalSizeByUser(@Param("userId") Long userId);

  @Query("select coalesce(sum(r.size),0) from UploadRecord r")
  long totalStorageBytes();

  @Query("select coalesce(sum(r.size),0) from UploadRecord r where r.uploadedAt >= :after")
  long totalSizeUploadedAfter(@Param("after") Instant after);

  @Query("select count(r) from UploadRecord r where r.uploadedAt >= :after")
  long countUploadedAfter(@Param("after") Instant after);

  @Query("select count(r) from UploadRecord r where r.uploadedAt >= :start and r.uploadedAt < :end")
  long countUploadedBetween(@Param("start") Instant start, @Param("end") Instant end);

  long countByViolationTrue();

  long countByViolationFalseAndUploadedAtAfter(Instant after);

  @Query("""
      select new com.chenxi.astrnest.upload.record.dto.UserUsageAggregate(
          r.user.id,
          count(r),
          coalesce(sum(r.size),0),
          coalesce(sum(r.likeCount),0)
      )
      from UploadRecord r
      where r.user.id in :userIds
      group by r.user.id
      """)
  List<UserUsageAggregate> aggregateUsageByUserIds(@Param("userIds") Collection<Long> userIds);

  void deleteByUserId(Long userId);

  Optional<UploadRecord> findByIdAndUserId(Long id, Long userId);

  Optional<UploadRecord> findByIdAndPublicAccessibleTrueAndViolationFalse(Long id);

  Optional<UploadRecord> findByObjectKey(String objectKey);

  Optional<UploadRecord> findByMediaUuid(String mediaUuid);

  Page<UploadRecord> findByPublicAccessibleTrueAndViolationFalse(Pageable pageable);

  @Query("""
      select distinct r from UploadRecord r
      left join fetch r.tags t
      where r.id in :ids
      """)
  List<UploadRecord> findWithTagsByIdIn(@Param("ids") Collection<Long> ids);

  @Query("""
      select r from UploadRecord r
      where (r.lastAccessAt IS NULL AND r.invokeCount = 0 AND r.uploadedAt < :threshold)
         OR (r.lastAccessAt IS NOT NULL AND r.lastAccessAt < :threshold)
      """)
  List<UploadRecord> findExpiredSince(@Param("threshold") Instant threshold);

  long countByPublicAccessibleTrueAndViolationFalse();

  @Query("select count(r) from UploadRecord r where r.apiKey is not null")
  long countApiUploads();

  @Query("select count(r) from UploadRecord r where r.apiKey is not null and r.uploadedAt >= :after")
  long countApiUploadsAfter(@Param("after") Instant after);

  @Query("""
      select new com.chenxi.astrnest.security.apikey.dto.ApiKeyUsageAggregate(
          r.apiKey.id,
          count(r),
          sum(case when r.uploadedAt >= :startOfDay then 1 else 0 end),
          max(r.uploadedAt)
      )
      from UploadRecord r
      where r.apiKey.id in :apiKeyIds
      group by r.apiKey.id
      """)
  List<ApiKeyUsageAggregate> aggregateApiUsageByKeyIds(
      @Param("apiKeyIds") Collection<Long> apiKeyIds,
      @Param("startOfDay") Instant startOfDay);

  List<UploadRecord> findTop3ByPublicAccessibleTrueAndViolationFalseOrderByLikeCountDesc();
}

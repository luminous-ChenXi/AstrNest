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
import org.springframework.data.jpa.repository.Modifying;
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

  List<UploadRecord> findByMediaUuidIn(Collection<String> mediaUuids);

  Page<UploadRecord> findByPublicAccessibleTrueAndViolationFalse(Pageable pageable);

  @Query("""
      select distinct r from UploadRecord r
      left join fetch r.tags t
      where r.id in :ids
      """)
  List<UploadRecord> findWithTagsByIdIn(@Param("ids") Collection<Long> ids);

  @Query("""
      select distinct r from UploadRecord r
      left join fetch r.album a
      where r.id in :ids
      """)
  List<UploadRecord> findWithAlbumByIdIn(@Param("ids") Collection<Long> ids);

  @Query("""
      select r from UploadRecord r
      where (r.lastAccessAt IS NULL AND r.invokeCount = 0 AND r.uploadedAt < :threshold)
         OR (r.lastAccessAt IS NOT NULL AND r.lastAccessAt < :threshold)
      """)
  List<UploadRecord> findExpiredSince(@Param("threshold") Instant threshold);

  long countByPublicAccessibleTrueAndViolationFalse();

  long countByAlbumIdAndPublicAccessibleTrueAndViolationFalse(Long albumId);

  List<UploadRecord> findByAlbumIdAndPublicAccessibleTrueAndViolationFalseOrderByUploadedAtDesc(Long albumId);

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

  /**
   * 查找用户上传的、不在指定图集中的图片
   */
  @Query("""
      select r from UploadRecord r
      where r.user.id = :userId
        and r.mediaUuid not in (
          select am.mediaUuid from AlbumMedia am where am.album.id = :albumId
        )
      order by r.uploadedAt desc
      """)
  Page<UploadRecord> findByUserIdAndNotInAlbum(
      @Param("userId") Long userId,
      @Param("albumId") Long albumId,
      Pageable pageable);

  /** 热门图片（审计复查点②）：对齐画廊 spec 的相册公开性条件——位于私有相册的公开图不上榜，防私有相册元数据外泄 */
  @Query("""
      select r from UploadRecord r
      where r.publicAccessible = true and r.violation = false
        and (not exists (select 1 from AlbumMedia am where am.mediaUuid = r.mediaUuid)
             or exists (select 1 from AlbumMedia am where am.mediaUuid = r.mediaUuid
                        and am.album.isPublic = true))
      order by r.likeCount desc
      """)
  List<UploadRecord> findTopPublicImages(Pageable pageable);

  /** 公开档案统计口径（审计复查点③）：只计公开∧非违规，私图/违规图不进匿名可见的计数 */
  @Query("""
      select new com.chenxi.astrnest.upload.record.dto.UserUsageAggregate(
          r.user.id,
          count(r),
          coalesce(sum(r.size),0),
          coalesce(sum(r.likeCount),0)
      )
      from UploadRecord r
      where r.user.id in :userIds and r.publicAccessible = true and r.violation = false
      group by r.user.id
      """)
  List<UserUsageAggregate> aggregatePublicUsageByUserIds(@Param("userIds") Collection<Long> userIds);

  /** 浏览量原子自增：先读后写在并发下丢更新（审计 P1-13）；计数失败不影响媒体访问 */
  @Modifying
  @Query("UPDATE UploadRecord r SET r.invokeCount = r.invokeCount + 1, r.lastAccessAt = :now WHERE r.objectKey = :objectKey")
  int incrementInvokeCount(@Param("objectKey") String objectKey, @Param("now") Instant now);
}

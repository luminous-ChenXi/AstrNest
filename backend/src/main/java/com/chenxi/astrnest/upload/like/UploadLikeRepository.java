package com.chenxi.astrnest.upload.like;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UploadLikeRepository extends JpaRepository<UploadLike, Long> {

  boolean existsByUploadRecordIdAndUserId(Long uploadId, Long userId);

  long countByUploadRecordId(Long uploadId);

  Optional<UploadLike> findByUploadRecordIdAndUserId(Long uploadId, Long userId);

  boolean existsByUploadRecordIdAndGuestToken(Long uploadId, String guestToken);

  Optional<UploadLike> findByUploadRecordIdAndGuestToken(Long uploadId, String guestToken);

  Optional<UploadLike> findFirstByUploadRecordIdOrderByLikedAtDesc(Long uploadId);

  @Query("select l.uploadRecord.id from UploadLike l where l.uploadRecord.id in :recordIds and l.user.id = :userId")
  List<Long> findLikedRecordIdsByUserId(@Param("recordIds") Collection<Long> recordIds, @Param("userId") Long userId);

  @Query("select l.uploadRecord.id from UploadLike l where l.uploadRecord.id in :recordIds and l.guestToken = :guestToken")
  List<Long> findLikedRecordIdsByGuestToken(@Param("recordIds") Collection<Long> recordIds, @Param("guestToken") String guestToken);

  @Query("""
      select l from UploadLike l
      where l.uploadRecord.id in :recordIds
      order by l.uploadRecord.id asc, l.likedAt desc
      """)
  List<UploadLike> findLatestLikesForUploads(@Param("recordIds") Collection<Long> recordIds);

  @Modifying
  @Query("delete from UploadLike l where l.uploadRecord.user.id = :userId")
  void deleteByUploadOwnerId(@Param("userId") Long userId);

  @Modifying
  void deleteByUserId(Long userId);
}

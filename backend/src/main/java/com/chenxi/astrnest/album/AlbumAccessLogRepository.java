package com.chenxi.astrnest.album;

import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AlbumAccessLogRepository extends JpaRepository<AlbumAccessLog, Long> {

  List<AlbumAccessLog> findByAlbumIdOrderByAccessedAtDesc(Long albumId, Pageable pageable);

  @Query("SELECT COUNT(l) FROM AlbumAccessLog l WHERE l.album.id = :albumId")
  long countByAlbumId(@Param("albumId") Long albumId);

  @Query("SELECT COUNT(l) FROM AlbumAccessLog l WHERE l.album.id = :albumId AND l.accessedAt >= :since")
  long countByAlbumIdAndAccessedAtAfter(@Param("albumId") Long albumId, @Param("since") Instant since);

  @Query(value = "SELECT media_uuid, COUNT(*) as count FROM album_access_logs " +
      "WHERE album_id = :albumId GROUP BY media_uuid ORDER BY count DESC LIMIT :limit",
      nativeQuery = true)
  List<Object[]> findMostAccessedMedia(@Param("albumId") Long albumId, @Param("limit") int limit);
}

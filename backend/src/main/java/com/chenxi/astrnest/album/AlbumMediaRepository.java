package com.chenxi.astrnest.album;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AlbumMediaRepository extends JpaRepository<AlbumMedia, Long> {

  List<AlbumMedia> findByAlbumIdOrderBySortOrderAsc(Long albumId);

  List<AlbumMedia> findByAlbumId(Long albumId);

  Optional<AlbumMedia> findByAlbumIdAndMediaUuid(Long albumId, String mediaUuid);

  boolean existsByAlbumIdAndMediaUuid(Long albumId, String mediaUuid);

  long countByAlbumId(Long albumId);

  @Query(value = "SELECT * FROM album_media WHERE album_id = :albumId ORDER BY RAND() LIMIT 1", nativeQuery = true)
  Optional<AlbumMedia> findRandomByAlbumId(@Param("albumId") Long albumId);

  @Query(value = "SELECT media_uuid FROM album_media WHERE album_id = :albumId", nativeQuery = true)
  List<String> findAllMediaUuidsByAlbumId(@Param("albumId") Long albumId);

  Page<AlbumMedia> findByAlbumIdOrderByIdAsc(Long albumId, Pageable pageable);

  void deleteByAlbumIdAndMediaUuid(Long albumId, String mediaUuid);

  // 获取图集的前N张图片（用于预览轮播）
  List<AlbumMedia> findTop3ByAlbumIdOrderBySortOrderAsc(Long albumId);
}

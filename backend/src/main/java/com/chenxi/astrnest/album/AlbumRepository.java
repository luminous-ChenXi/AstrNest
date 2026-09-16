package com.chenxi.astrnest.album;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AlbumRepository extends JpaRepository<Album, Long> {

  Optional<Album> findByPathSlug(String pathSlug);

  Optional<Album> findByAlbumUuid(String albumUuid);

  List<Album> findByUserIdAndIsPublicTrue(Long userId);

  List<Album> findByUserId(Long userId);

  Page<Album> findByUserId(Long userId, Pageable pageable);

  boolean existsByPathSlug(String pathSlug);

  boolean existsByPathSlugAndIdNot(String pathSlug, Long id);

  @Modifying
  @Query("UPDATE Album a SET a.accessCount = a.accessCount + 1 WHERE a.id = :albumId")
  void incrementAccessCount(@Param("albumId") Long albumId);

  @Query("SELECT COUNT(a) FROM Album a WHERE a.user.id = :userId")
  long countByUserId(@Param("userId") Long userId);

  /**
   * 查询所有公开图集
   */
  List<Album> findByIsPublicTrue();
}

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

  /**
   * 热门图集聚合（审计 P2-13）：图集内公开未违规媒体的点赞总和 + 媒体数，按点赞降序。
   * 此前为全表载入公开图集/媒体/上传记录后内存求和排序；分页参数用于取 Top N。
   */
  @org.springframework.data.jpa.repository.Query(value = """
      select am.album_id,
             count(*) as media_count,
             coalesce(sum(case when r.is_public = 1 and r.is_violation = 0 then r.like_count else 0 end), 0) as total_likes
      from album_media am
      join albums a on a.id = am.album_id
      left join upload_records r on r.media_uuid = am.media_uuid
      where a.is_public = 1
      group by am.album_id
      order by total_likes desc
      """, nativeQuery = true)
  List<Object[]> aggregateFeatured(org.springframework.data.domain.Pageable pageable);
}

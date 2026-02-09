package com.chenxi.astrnest.album.dto;

import java.time.Instant;
import lombok.Data;

/**
 * 首页Featured图集响应DTO
 * 包含图集信息和图集内所有图片的喜欢数总和
 */
@Data
public class AlbumFeaturedResponse {

  private Long id;
  private String albumUuid;
  private String pathSlug;
  private String name;
  private String description;
  private String coverImageUuid;
  private Long mediaCount;
  private Long totalLikes;
  private String username;
  private Instant createdAt;
}

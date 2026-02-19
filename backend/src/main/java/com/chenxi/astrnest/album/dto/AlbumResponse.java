package com.chenxi.astrnest.album.dto;

import java.time.Instant;
import java.util.List;
import lombok.Data;

@Data
public class AlbumResponse {

  private Long id;
  private String albumUuid;
  private String pathSlug;
  private String name;
  private String description;
  private Boolean isPublic;
  private String coverImageUuid;
  private Long accessCount;
  private Long mediaCount;
  private Long userId;
  private String username;
  private Instant createdAt;
  private Instant updatedAt;

  // 权限相关字段（仅在特定接口返回）
  private Boolean isOwner;
  private Boolean isAdmin;
  private Boolean canEdit;
  private Boolean canAddMedia;

  // 预览图片列表（最多3张，用于卡片轮播展示）
  private List<String> previewImageUuids;
}

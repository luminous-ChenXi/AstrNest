package com.chenxi.astrnest.album.dto;

import java.time.Instant;
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
}

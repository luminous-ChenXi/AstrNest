package com.chenxi.astrnest.album.dto;

import java.time.Instant;
import lombok.Data;

@Data
public class AlbumMediaResponse {

  private Long id;
  private String mediaUuid;
  private String fileName;
  private String publicUrl;
  private String thumbnailUrl;
  private String contentType;
  private Long size;
  private Integer width;
  private Integer height;
  private Instant addedAt;
  private Integer sortOrder;
}

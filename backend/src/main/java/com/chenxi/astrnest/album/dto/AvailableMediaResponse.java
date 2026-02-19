package com.chenxi.astrnest.album.dto;

import java.time.Instant;
import lombok.Builder;
import lombok.Data;

/**
 * 可添加到图集的图片响应DTO
 */
@Data
@Builder
public class AvailableMediaResponse {

  private Long id;
  private String mediaUuid;
  private String fileName;
  private String publicUrl;
  private String thumbnailUrl;
  private String contentType;
  private long size;
  private Integer width;
  private Integer height;
  private Instant uploadedAt;
  private boolean publicAccessible;
  private boolean violation;
}

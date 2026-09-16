package com.chenxi.astrnest.album.dto;

import java.util.List;
import lombok.Data;

@Data
public class AlbumDetailResponse {

  private AlbumResponse album;
  private List<AlbumMediaResponse> medias;
  private Long totalMedia;
}

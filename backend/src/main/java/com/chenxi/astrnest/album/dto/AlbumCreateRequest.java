package com.chenxi.astrnest.album.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AlbumCreateRequest {

  @NotBlank(message = "路径标识不能为空")
  @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "路径标识只能包含字母、数字、下划线和横线")
  @Size(min = 1, max = 50, message = "路径标识长度必须在1-50之间")
  private String pathSlug;

  @NotBlank(message = "图集名称不能为空")
  @Size(max = 100, message = "图集名称长度不能超过100")
  private String name;

  @Size(max = 500, message = "描述长度不能超过500")
  private String description;

  private Boolean isPublic = true;
}

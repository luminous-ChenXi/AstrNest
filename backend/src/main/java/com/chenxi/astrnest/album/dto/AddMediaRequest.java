package com.chenxi.astrnest.album.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AddMediaRequest {

  @NotBlank(message = "图片UUID不能为空")
  @Size(max = 36, message = "图片UUID长度不能超过36")
  private String mediaUuid;
}

package com.chenxi.astrnest.tag.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateChenxiTagRequest(
    @NotBlank(message = "请输入标签名称")
    @Size(max = 20, message = "标签名称最多 20 个字符")
    String name,

    @Size(max = 255, message = "描述内容最多 255 个字符")
    String description
) {
}

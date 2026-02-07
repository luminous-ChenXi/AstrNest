package com.imgbed.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
    @NotBlank(message = "昵称不能为空") String displayName,
    @Size(max = 512, message = "头像链接过长") String avatarUrl,
    @Size(max = 255, message = "网站地址过长") String website,
    @Size(max = 255, message = "签名长度超限") String signature,
    @Size(max = 120, message = "地址长度超限") String location
) {}

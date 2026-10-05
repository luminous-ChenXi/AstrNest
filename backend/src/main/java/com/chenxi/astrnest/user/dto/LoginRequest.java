package com.chenxi.astrnest.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
    @NotBlank(message = "用户名不能为空") String username,
    // 长度上限：/login 匿名可达，无上限的超长密码体反复触发 BCrypt(12) 可打满 CPU
    @NotBlank(message = "密码不能为空") @Size(max = 64, message = "密码过长") String password
) {}

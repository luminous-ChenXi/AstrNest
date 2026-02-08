package com.chenxi.astrnest.user.dto;

import jakarta.validation.constraints.NotBlank;

public record ChangePasswordRequest(
    @NotBlank(message = "请输入当前密码") String currentPassword,
    @NotBlank(message = "请输入新密码") String newPassword,
    @NotBlank(message = "请再次输入新密码") String confirmPassword
) {}

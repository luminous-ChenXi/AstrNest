package com.chenxi.astrnest.chenxi.auth.dto;

import com.chenxi.astrnest.chenxi.auth.ChenxiEmailScene;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record VerifyEmailCodeRequest(
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    String email,

    @NotBlank(message = "验证码不能为空")
    String code,

    @NotNull(message = "场景不能为空")
    ChenxiEmailScene scene
) {}

package com.chenxi.astrnest.chenxi.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VerifyEmailCodeRequest(
    @NotBlank(message = "请输入邮箱地址")
    @Email(message = "邮箱格式不正确")
    String email,

    @NotBlank(message = "请输入验证码")
    @Size(min = 6, max = 6, message = "验证码为 6 位数字")
    String code
) {
}

package com.chenxi.astrnest.chenxi.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 本地注册请求体。email/code/linkToken 的必填性由站点开关
 * registration.email_verify_required 在服务端裁决（开启：验证码或链接令牌二选一必填）。
 */
public record RegisterAccountRequest(
    @Email(message = "邮箱格式不正确") @Size(max = 180) String email,
    @Size(min = 6, max = 6, message = "验证码为 6 位数字") String code,
    @Size(max = 64, message = "链接令牌过长") String linkToken,
    @NotBlank @Size(min = 4, max = 32) String username,
    @NotBlank @Size(min = 8, max = 64) String password,
    @Size(max = 64) String displayName
) {
}

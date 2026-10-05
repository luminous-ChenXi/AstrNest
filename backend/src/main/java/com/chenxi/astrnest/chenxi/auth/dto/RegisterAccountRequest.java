package com.chenxi.astrnest.chenxi.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 本地注册请求体。email/code/linkToken 的必填性由站点开关
 * registration.email_verify_required 在服务端裁决（开启：验证码或链接令牌二选一必填）。
 */
public record RegisterAccountRequest(
    @Email(message = "邮箱格式不正确") @Size(max = 180) String email,
    // 验证码仅在「注册邮箱验证」开启时必填（服务端裁决）；@Size 不约束空串以兼容开关关闭态
    @Size(max = 6, message = "验证码为 6 位数字") String code,
    @Size(max = 64, message = "链接令牌过长") String linkToken,
    // 用户名字符白名单与安装向导同一口径（InstallSetupService），防止 HTML 元字符/控制字符入库
    @NotBlank @Size(min = 4, max = 32)
    @Pattern(regexp = "^[A-Za-z0-9_.-]+$", message = "用户名仅支持字母、数字、点、下划线和连字符")
    String username,
    @NotBlank @Size(min = 8, max = 64) String password,
    @Size(max = 64) String displayName
) {
}

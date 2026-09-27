package com.chenxi.astrnest.install;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * POST /api/install/site-config 请求体：安装向导「站点配置」步骤的可选初始配置。
 *
 * <p>全部字段可空：null 表示"不修改，保持系统默认"，前端"跳过此步"时直接提交空对象（或干脆不调用）。
 * 写入目标是 system_config 单行配置（id=1），语义与安装完成后的管理后台一致。</p>
 *
 * @param registrationEnabled        是否开放邮箱注册（默认 false）
 * @param guestUploadEnabled         是否允许访客上传（默认 false）
 * @param maxUploadMb                单文件上传上限（MB，1-512；默认 20）
 * @param assetDomain                资源加速域名（可空，带 http(s):// 前缀时自动规范化）
 * @param customFooterHtml           自定义页脚 HTML（可空）
 * @param registrationEmailVerifyRequired 注册邮箱验证开关（默认 false；开启需先配置 SMTP）
 * @param loginTotpRequired          登录二步验证（TOTP）开关（默认 false）
 */
public record InstallSiteConfigRequest(
    Boolean registrationEnabled,
    Boolean guestUploadEnabled,
    @Min(value = 1, message = "单文件上限至少 1 MB") @Max(value = 512, message = "单文件上限不能超过 512 MB")
    Integer maxUploadMb,
    String assetDomain,
    String customFooterHtml,
    Boolean registrationEmailVerifyRequired,
    Boolean loginTotpRequired) {
}

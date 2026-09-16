package com.chenxi.astrnest.install;

/**
 * POST /api/install/admin 请求体：初始管理员账号信息。
 * 校验在 InstallSetupService 中完成（风格与 ChenxiAuthService 一致）。
 */
public record InstallAdminRequest(String username, String email, String password, String displayName) {
}

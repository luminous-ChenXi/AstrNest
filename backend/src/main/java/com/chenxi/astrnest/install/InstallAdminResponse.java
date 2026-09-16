package com.chenxi.astrnest.install;

/**
 * POST /api/install/admin 响应：创建成功的账号摘要（不回传密码）。
 */
public record InstallAdminResponse(String username, String email, String displayName, String role) {
}

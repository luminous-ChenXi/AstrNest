package com.chenxi.astrnest.install;

/**
 * POST /api/install/reset 响应：安装状态重置结果。
 */
public record InstallResetResponse(boolean success, String message) {
}

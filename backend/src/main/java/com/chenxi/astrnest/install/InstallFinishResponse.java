package com.chenxi.astrnest.install;

/**
 * POST /api/install/finish 响应：完成标记写入结果。
 */
public record InstallFinishResponse(boolean success, String installedAt, String message) {
}

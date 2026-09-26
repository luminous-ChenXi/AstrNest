package com.chenxi.astrnest.install;

/**
 * POST /api/install/site-config 响应：站点配置保存结果。
 */
public record InstallSiteConfigResponse(boolean success, String message) {
}

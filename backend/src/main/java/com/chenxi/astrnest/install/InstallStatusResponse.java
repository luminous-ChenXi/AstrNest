package com.chenxi.astrnest.install;

import java.util.List;

/**
 * GET /api/install/status 响应：安装状态 + 环境检测项列表。
 *
 * @param installed   users 表存在且有用户
 * @param schemaState NOT_INSTALLED / EMPTY / INSTALLED
 * @param finished    向导完成标记（install_state）已写入
 * @param checks      环境检测项
 */
public record InstallStatusResponse(
    boolean installed,
    String schemaState,
    boolean finished,
    List<InstallCheckItem> checks) {
}

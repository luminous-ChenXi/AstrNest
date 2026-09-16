package com.chenxi.astrnest.install;

import java.util.List;

/**
 * POST /api/install/database 响应：执行 install-schema.sql 的摘要。
 */
public record InstallDatabaseResponse(
    boolean success,
    int executedStatements,
    int tablesCreated,
    int skippedStatements,
    List<String> errors
) {
}

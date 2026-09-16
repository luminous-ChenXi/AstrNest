package com.chenxi.astrnest.install;

import java.util.Map;

/**
 * 安装状态快照（纯 JDBC 探测结果，绝不触发 JPA）。
 *
 * <p>installed 的唯一定义：users 表存在且用户数大于 0。</p>
 *
 * @param dbReachable 数据库是否可连接
 * @param dbVersion   MySQL 版本号（SELECT VERSION()），不可连接时为 null
 * @param dbError     数据库连接失败原因（可读消息）
 * @param schemaState NOT_INSTALLED / EMPTY / INSTALLED
 * @param installed   是否已完成安装（users 表存在且有用户）
 * @param userCount   users 表行数（users 表不存在时为 0）
 * @param coreTables  核心表存在性概览（users/roles/upload_records/albums）
 * @param totalTables 当前库中表数量
 */
public record InstallSnapshot(
    boolean dbReachable,
    String dbVersion,
    String dbError,
    String schemaState,
    boolean installed,
    long userCount,
    Map<String, Boolean> coreTables,
    int totalTables
) {
}

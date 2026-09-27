package com.chenxi.astrnest.install;

/**
 * POST /api/install/database/test 响应：数据库连接测试结果。
 *
 * @param success         连接是否成功
 * @param message         人读结果描述（成功含版本与字符集；失败给出具体原因与排查建议）
 * @param mysqlVersion    MySQL 版本号（成功时返回）
 * @param charsetServer   服务器字符集（character_set_server，成功时返回）
 * @param charsetDatabase 目标库字符集（character_set_database，成功时返回）
 * @param databaseCreated 本次调用是否执行了「尝试创建数据库」
 * @param errorCode       失败分类：CONNECTION_REFUSED / AUTH_FAILED / DB_MISSING / UNKNOWN，成功时为 null
 * @param runtimeJdbcUrl  当前后端运行时实际使用的 JDBC 连接串（用于提示表单与运行时是否一致）
 * @param matchesRuntime  表单连接目标与运行时连接是否一致（不一致时初始化作用于运行时库，需先改配置重启）
 */
public record InstallDbTestResponse(
    boolean success,
    String message,
    String mysqlVersion,
    String charsetServer,
    String charsetDatabase,
    boolean databaseCreated,
    String errorCode,
    String runtimeJdbcUrl,
    boolean matchesRuntime) {
}

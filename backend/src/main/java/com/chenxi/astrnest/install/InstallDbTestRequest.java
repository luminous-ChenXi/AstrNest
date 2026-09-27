package com.chenxi.astrnest.install;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * POST /api/install/database/test 请求体：安装向导「数据库配置」步骤的连接参数。
 *
 * @param location        数据库位置：local（本机，自动预填 localhost:3306）/ remote（远程主机）
 * @param host            主机地址（location=local 时不填默认 localhost）
 * @param port            端口（默认 3306）
 * @param databaseName    数据库名称
 * @param username        数据库用户名
 * @param password        数据库密码（可空）
 * @param createIfMissing 库不存在时是否尝试创建（需要账号具备建库权限）
 */
public record InstallDbTestRequest(
    String location,
    String host,
    @Min(value = 1, message = "端口最小为 1") @Max(value = 65535, message = "端口最大为 65535")
    Integer port,
    String databaseName,
    String username,
    String password,
    boolean createIfMissing) {
}

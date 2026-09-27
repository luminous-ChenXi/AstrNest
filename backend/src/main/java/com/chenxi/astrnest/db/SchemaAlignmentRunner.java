package com.chenxi.astrnest.db;

import java.sql.DatabaseMetaData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SchemaAlignmentRunner implements ApplicationRunner {

  private final JdbcTemplate jdbcTemplate;

  @Override
  public void run(ApplicationArguments args) {
    if (!isMySql()) {
      // 本类是 MySQL 专用的存量库对齐（CHANGE COLUMN / ADD UNIQUE KEY / information_schema.STATISTICS
      // 等语法与字典表为 MySQL 特有）；H2 等非 MySQL 环境（如单元测试内嵌库）直接跳过，
      // 避免 SQL 方言不兼容导致启动失败。生产 MySQL 行为不受影响。
      log.info("Skipping MySQL schema alignment: datasource is not MySQL");
      return;
    }
    alignColumn("upload_records", "object_key", "storage_path", "VARCHAR(255) NOT NULL");
    alignColumn("upload_records", "public_url", "image_link", "VARCHAR(255) NOT NULL");
    alignColumn("upload_records", "image_name", "file_name", "VARCHAR(180) NOT NULL");

    backfillAndDropLegacy("upload_records", "object_key", "storage_path");
    backfillAndDropLegacy("upload_records", "public_url", "image_link");
    backfillAndDropLegacy("upload_records", "image_name", "file_name");

    ensureColumnDataType("chenxi_mail_template", "content", "LONGTEXT", false);
    ensureColumnDataType("chenxi_mail_template", "variables_json", "LONGTEXT", true);
    ensureColumnExists("upload_records", "last_access_at", "DATETIME NULL AFTER invoke_count");
    ensureColumnExists("system_config", "auto_cleanup_days", "INT NOT NULL DEFAULT 30 AFTER guest_like_enabled");

    // 站长安全开关（默认关闭，存量库补齐；全新库由 install-schema.sql 直接创建）
    ensureColumnExists("system_config", "registration_email_verify_required",
        "BIT(1) NOT NULL DEFAULT b'0' AFTER registration_enabled");
    ensureColumnExists("system_config", "login_totp_required",
        "BIT(1) NOT NULL DEFAULT b'0' AFTER registration_email_verify_required");

    // SSO 影子账号列（chenxi.passport 默认关闭，仅外部身份源登录时写入数据，结构始终补齐）
    ensureColumnExists("users", "sso_sub", "VARCHAR(64) NULL AFTER avatar_url");
    ensureColumnExists("users", "identity_source", "VARCHAR(32) NOT NULL DEFAULT 'local' AFTER sso_sub");
    ensureIndexExists("users", "uk_users_sso_sub", "ALTER TABLE users ADD UNIQUE KEY uk_users_sso_sub (sso_sub)");

    // 注册邮箱验证（registration.email_verify_required 开关的落地字段）：
    // 存量用户按「已验证」补齐——历史注册流程本身强制邮箱验证码
    ensureColumnExists("users", "email_verified", "BIT(1) NOT NULL DEFAULT b'1' AFTER active");

    // 注册验证链接令牌（注册邮件「点链接验证」入口，与验证码同生共死）
    ensureColumnExists("chenxi_email_token", "link_token", "VARCHAR(64) NULL AFTER captcha_token");

    // 用户 TOTP（登录二步验证）绑定表（login.totp_required 开关的落地存储）
    ensureUserTotpTable();

    // 安装向导完成标记表（install 包使用；全新库由 install-schema.sql 创建，旧库在此补齐）
    ensureInstallStateTable();
  }

  /** 数据源是否为 MySQL（H2 内嵌库等环境跳过 MySQL 专用对齐）。探测失败按 MySQL 处理，维持原行为。 */
  private boolean isMySql() {
    try {
      DatabaseMetaData metaData = jdbcTemplate.getDataSource().getConnection().getMetaData();
      String product = metaData == null ? null : metaData.getDatabaseProductName();
      return product == null || product.toLowerCase().contains("mysql");
    } catch (Exception exception) {
      return true;
    }
  }

  /**
   * 按现有模式条件创建 install_state 表（表不存在才建，失败仅告警不阻断启动）。
   */
  private void ensureInstallStateTable() {
    try {
      Boolean exists = jdbcTemplate.queryForObject(
          "SELECT COUNT(*) > 0 FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'install_state'",
          Boolean.class
      );
      if (Boolean.TRUE.equals(exists)) {
        return;
      }
      jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS install_state ("
          + "id BIGINT PRIMARY KEY, "
          + "installed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP"
          + ") ENGINE = InnoDB DEFAULT CHARSET = utf8mb4");
      log.info("Created missing table install_state");
    } catch (Exception exception) {
      log.warn("Failed to ensure install_state table: {}", exception.getMessage());
    }
  }

  /**
   * 按现有模式条件创建 user_totp 表（表不存在才建，失败仅告警不阻断启动）。
   */
  private void ensureUserTotpTable() {
    try {
      Boolean exists = jdbcTemplate.queryForObject(
          "SELECT COUNT(*) > 0 FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_totp'",
          Boolean.class
      );
      if (Boolean.TRUE.equals(exists)) {
        return;
      }
      jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS user_totp ("
          + "user_id BIGINT NOT NULL PRIMARY KEY, "
          + "secret VARCHAR(64) NOT NULL, "
          + "confirmed BIT(1) NOT NULL DEFAULT b'0', "
          + "recovery_hashes TEXT NULL, "
          + "last_used_counter BIGINT NULL, "
          + "created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6), "
          + "confirmed_at DATETIME(6) NULL, "
          + "updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6), "
          + "CONSTRAINT fk_user_totp_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE"
          + ") ENGINE = InnoDB DEFAULT CHARSET = utf8mb4");
      log.info("Created missing table user_totp");
    } catch (Exception exception) {
      log.warn("Failed to ensure user_totp table: {}", exception.getMessage());
    }
  }

  private void alignColumn(String tableName, String legacyName, String desiredName, String definition) {
    if (!columnExists(tableName, legacyName)) {
      return;
    }
    if (columnExists(tableName, desiredName)) {
      return;
    }
    String statement =
        "ALTER TABLE " + tableName + " CHANGE COLUMN " + legacyName + " " + desiredName + " " + definition;
    try {
      jdbcTemplate.execute(statement);
      log.info("Aligned column {}.{} -> {}", tableName, legacyName, desiredName);
    } catch (Exception exception) {
      log.warn("Failed to align column {}.{} -> {}: {}", tableName, legacyName, desiredName,
          exception.getMessage());
    }
  }

  private void backfillAndDropLegacy(String tableName, String legacyName, String desiredName) {
    if (!columnExists(tableName, legacyName)) {
      return;
    }
    if (!columnExists(tableName, desiredName)) {
      log.warn("Skipping legacy cleanup for {}.{} because {} is missing", tableName, legacyName, desiredName);
      return;
    }
    String updateSql =
        "UPDATE " + tableName + " SET " + desiredName + " = " + legacyName + " WHERE (" + desiredName
            + " IS NULL OR " + desiredName + " = '') AND " + legacyName + " IS NOT NULL";
    try {
      jdbcTemplate.execute(updateSql);
    } catch (Exception exception) {
      log.warn("Failed to backfill {}.{} from {}: {}", tableName, desiredName, legacyName,
          exception.getMessage());
    }
    String dropSql = "ALTER TABLE " + tableName + " DROP COLUMN " + legacyName;
    try {
      jdbcTemplate.execute(dropSql);
      log.info("Dropped legacy column {}.{}", tableName, legacyName);
    } catch (Exception exception) {
      log.warn("Failed to drop legacy column {}.{}: {}", tableName, legacyName, exception.getMessage());
    }
  }

  private boolean columnExists(String tableName, String columnName) {
    Boolean exists = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) > 0 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
        Boolean.class,
        tableName,
        columnName
    );
    return Boolean.TRUE.equals(exists);
  }

  private void ensureColumnDataType(String tableName, String columnName, String desiredDataType, boolean nullable) {
    if (!columnExists(tableName, columnName)) {
      return;
    }
    String dataType = jdbcTemplate.queryForObject(
        "SELECT DATA_TYPE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
        String.class,
        tableName,
        columnName
    );
    if (dataType != null && dataType.equalsIgnoreCase(desiredDataType)) {
      return;
    }
    String nullClause = nullable ? "NULL" : "NOT NULL";
    String statement =
        "ALTER TABLE " + tableName + " MODIFY COLUMN " + columnName + " " + desiredDataType + " " + nullClause;
    try {
      jdbcTemplate.execute(statement);
      log.info("Aligned data type for {}.{} to {}", tableName, columnName, desiredDataType);
    } catch (Exception exception) {
      log.warn("Failed to align data type for {}.{}: {}", tableName, columnName, exception.getMessage());
    }
  }

  private void ensureColumnExists(String tableName, String columnName, String definition) {
    if (columnExists(tableName, columnName)) {
      return;
    }
    String statement = "ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + definition;
    try {
      jdbcTemplate.execute(statement);
      log.info("Added missing column {}.{}", tableName, columnName);
    } catch (Exception exception) {
      log.warn("Failed to add column {}.{}: {}", tableName, columnName, exception.getMessage());
    }
  }

  private void ensureIndexExists(String tableName, String indexName, String definition) {
    Boolean exists = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) > 0 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND INDEX_NAME = ?",
        Boolean.class,
        tableName,
        indexName
    );
    if (Boolean.TRUE.equals(exists)) {
      return;
    }
    try {
      jdbcTemplate.execute(definition);
      log.info("Added missing index {}.{}", tableName, indexName);
    } catch (Exception exception) {
      log.warn("Failed to add index {}.{}: {}", tableName, indexName, exception.getMessage());
    }
  }
}

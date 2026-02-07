package com.chenxi.astrnest.db;

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
}

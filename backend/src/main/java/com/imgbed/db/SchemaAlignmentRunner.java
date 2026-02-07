package com.imgbed.db;

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
}

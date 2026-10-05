package com.chenxi.astrnest.db;

import java.nio.charset.StandardCharsets;
import java.sql.DatabaseMetaData;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

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

    // 令牌版本（JWT 服务端吊销：改密/找回密码 +1 使该用户旧令牌全部失效）
    ensureColumnExists("users", "token_version", "BIGINT NOT NULL DEFAULT 0 AFTER email_verified");
    // 兼容由 Hibernate ddl-auto 建出的无默认值列（历史 dev 库）：统一补默认值，
    // 否则安装向导的原生 INSERT（列清单不含 token_version）在严格模式下报 1364
    try {
      jdbcTemplate.execute("ALTER TABLE users MODIFY COLUMN token_version BIGINT NOT NULL DEFAULT 0");
    } catch (Exception exception) {
      log.warn("Failed to align users.token_version default: {}", exception.getMessage());
    }

    // 验证码/图形验证码列宽 6/16 → 64：明文改 SHA-256 哈希入库后需要 64 位十六进制
    widenColumnIfSmaller("chenxi_email_token", "code", 64);
    widenColumnIfSmaller("chenxi_captcha_ticket", "captcha_code", 64);

    // 注册验证链接令牌（注册邮件「点链接验证」入口，与验证码同生共死）
    ensureColumnExists("chenxi_email_token", "link_token", "VARCHAR(64) NULL AFTER captcha_token");

    // 用户 TOTP（登录二步验证）绑定表（login.totp_required 开关的落地存储）
    ensureUserTotpTable();

    alignAuditColumns();
    alignQueryIndexes();
    alignTagSlugNotNull();

    // 安装向导完成标记表（install 包使用；全新库由 install-schema.sql 创建，旧库在此补齐）
    ensureInstallStateTable();
  }

  /**
   * 审计字段补齐（与 install-schema.sql 同步；存量库由 Hibernate validate 启动失败前兜底）：
   * users/upload_records/roles/api_keys/content_policy 的 updated_at 等列。
   * 全部为「缺列才补」，DATETIME(6) + DEFAULT/ON UPDATE CURRENT_TIMESTAMP(6) 风格。
   */
  private void alignAuditColumns() {
    ensureColumnExists("users", "updated_at",
        "DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) AFTER created_at");
    ensureColumnExists("upload_records", "updated_at",
        "DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) AFTER uploaded_at");
    ensureColumnExists("roles", "created_at",
        "DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) AFTER description");
    ensureColumnExists("roles", "updated_at",
        "DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) AFTER created_at");
    ensureColumnExists("api_keys", "updated_at",
        "DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) AFTER created_at");
    ensureColumnExists("content_policy", "updated_at",
        "DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) AFTER webhook_url");
  }

  /**
   * 查询索引补齐/整合（与 install-schema.sql 同步）：
   * 高频查询路径补复合索引；遗留单列索引由等价复合索引（左前缀覆盖 FK）取代后删除；
   * 反面教材 idx_upload_records_width/height（低基数、无查询引用）直接删除。
   */
  private void alignQueryIndexes() {
    ensureIndexExists("upload_records", "idx_upload_records_uploaded_at",
        "ALTER TABLE upload_records ADD INDEX idx_upload_records_uploaded_at (uploaded_at)");
    ensureIndexExists("upload_records", "idx_upload_records_public_time",
        "ALTER TABLE upload_records ADD INDEX idx_upload_records_public_time (is_public, is_violation, uploaded_at)");
    ensureIndexExists("upload_records", "idx_upload_records_media_type",
        "ALTER TABLE upload_records ADD INDEX idx_upload_records_media_type (media_type)");
    ensureIndexExists("upload_likes", "idx_upload_likes_user_time",
        "ALTER TABLE upload_likes ADD INDEX idx_upload_likes_user_time (user_id, liked_at)");
    ensureIndexExists("chenxi_email_token", "idx_chenxi_email_token_expires",
        "ALTER TABLE chenxi_email_token ADD INDEX idx_chenxi_email_token_expires (expires_at)");
    ensureIndexExists("chenxi_captcha_ticket", "idx_chenxi_captcha_ticket_expires",
        "ALTER TABLE chenxi_captcha_ticket ADD INDEX idx_chenxi_captcha_ticket_expires (expires_at)");
    ensureIndexExists("security_logs", "idx_security_logs_username_created",
        "ALTER TABLE security_logs ADD INDEX idx_security_logs_username_created (username, created_at)");

    // 单列 → 复合整合：先建复合（左前缀满足 FK 索引要求），成功后删除旧单列索引
    ensureIndexExists("album_access_logs", "idx_album_access_logs_album_time",
        "ALTER TABLE album_access_logs ADD INDEX idx_album_access_logs_album_time (album_id, accessed_at)");
    dropIndexIfExists("album_access_logs", "idx_album_access_logs_album_id");
    dropIndexIfExists("album_access_logs", "idx_album_access_logs_accessed_at");
    ensureIndexExists("user_login_events", "idx_user_login_events_user_time",
        "ALTER TABLE user_login_events ADD INDEX idx_user_login_events_user_time (user_id, occurred_at)");
    dropIndexIfExists("user_login_events", "idx_user_login_events_user");

    // 反面教材：width/height 单列索引无查询引用且低基数，只增写入开销
    dropIndexIfExists("upload_records", "idx_upload_records_width");
    dropIndexIfExists("upload_records", "idx_upload_records_height");
  }

  /**
   * tags.slug 收紧为 NOT NULL：先按实体同款算法（小写连字符，非 ASCII 回退 md5 前缀）
   * 一次性回填存量空 slug（含查重后缀），成功后 MODIFY 收紧。回填/收紧失败仅告警不阻断。
   */
  private void alignTagSlugNotNull() {
    if (!columnExists("tags", "slug")) {
      return;
    }
    if (!backfillTagSlugs()) {
      log.warn("Skipping tags.slug NOT NULL tighten: slug backfill incomplete");
      return;
    }
    String nullable = jdbcTemplate.queryForObject(
        "SELECT IS_NULLABLE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tags' AND COLUMN_NAME = 'slug'",
        String.class
    );
    if ("NO".equalsIgnoreCase(nullable)) {
      return;
    }
    try {
      jdbcTemplate.execute("ALTER TABLE tags MODIFY COLUMN slug VARCHAR(180) NOT NULL");
      log.info("Aligned tags.slug to NOT NULL");
    } catch (Exception exception) {
      log.warn("Failed to tighten tags.slug to NOT NULL: {}", exception.getMessage());
    }
  }

  /** 存量空 slug 回填；返回是否已无空 slug。 */
  private boolean backfillTagSlugs() {
    try {
      boolean hasEmpty = jdbcTemplate.queryForObject(
          "SELECT COUNT(*) > 0 FROM tags WHERE slug IS NULL OR slug = ''", Boolean.class);
      if (!Boolean.TRUE.equals(hasEmpty)) {
        return true;
      }
      Set<String> usedSlugs = new HashSet<>();
      jdbcTemplate.query("SELECT slug FROM tags WHERE slug IS NOT NULL AND slug <> ''",
          rs -> {
            usedSlugs.add(rs.getString(1).toLowerCase(Locale.ROOT));
          });
      List<TagRow> rows = new ArrayList<>();
      jdbcTemplate.query("SELECT id, name FROM tags WHERE slug IS NULL OR slug = ''", rs -> {
        rows.add(new TagRow(rs.getLong(1), rs.getString(2)));
      });
      for (TagRow row : rows) {
        String slug = generateTagSlug(row.name());
        String candidate = slug;
        int suffix = 2;
        while (candidate == null || !usedSlugs.add(candidate.toLowerCase(java.util.Locale.ROOT))) {
          candidate = slug + "-" + suffix++;
        }
        jdbcTemplate.update("UPDATE tags SET slug = ? WHERE id = ?", candidate, row.id());
        log.info("Backfilled tags.slug for id={} name={}: {}", row.id(), row.name(), candidate);
      }
      Boolean remaining = jdbcTemplate.queryForObject(
          "SELECT COUNT(*) > 0 FROM tags WHERE slug IS NULL OR slug = ''", Boolean.class);
      return !Boolean.TRUE.equals(remaining);
    } catch (Exception exception) {
      log.warn("Failed to backfill tags.slug: {}", exception.getMessage());
      return false;
    }
  }

  /** 与 ChenxiTag#generateSlug 同款算法，保证应用后续生成的 slug 与回填口径一致。 */
  private String generateTagSlug(String source) {
    if (source == null || source.isBlank()) {
      return null;
    }
    String ascii = Normalizer.normalize(source, Normalizer.Form.NFD)
        .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
        .toLowerCase(Locale.ROOT)
        .replaceAll("[^a-z0-9]+", "-")
        .replaceAll("-+", "-")
        .replaceAll("^-|-$", "");
    if (!ascii.isBlank()) {
      return ascii;
    }
    String hash = DigestUtils.md5DigestAsHex(
        source.trim().toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8));
    return "tag-" + hash.substring(0, Math.min(12, hash.length()));
  }

  /** slug 回填行载体（id + 原始名称）。 */
  private record TagRow(Long id, String name) {
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

  /** 列宽不足时扩容（如验证码列 6/16 → 64：明文改 SHA-256 哈希入库后需要 64 位十六进制） */
  private void widenColumnIfSmaller(String tableName, String columnName, int targetLength) {
    if (!columnExists(tableName, columnName)) {
      return;
    }
    Integer currentLength = jdbcTemplate.queryForObject(
        "SELECT CHARACTER_MAXIMUM_LENGTH FROM information_schema.COLUMNS "
            + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
        Integer.class,
        tableName,
        columnName
    );
    if (currentLength == null || currentLength >= targetLength) {
      return;
    }
    try {
      String isNullable = jdbcTemplate.queryForObject(
          "SELECT IS_NULLABLE FROM information_schema.COLUMNS "
              + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
          String.class,
          tableName,
          columnName
      );
      String nullClause = "NO".equalsIgnoreCase(isNullable) ? "NOT NULL" : "NULL";
      jdbcTemplate.execute("ALTER TABLE " + tableName + " MODIFY COLUMN " + columnName
          + " VARCHAR(" + targetLength + ") " + nullClause);
      log.info("Widened column {}.{} to VARCHAR({})", tableName, columnName, targetLength);
    } catch (Exception exception) {
      log.warn("Failed to widen column {}.{}: {}", tableName, columnName, exception.getMessage());
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

  /** 删除遗留/反面教材索引（不存在则静默跳过，失败仅告警不阻断启动）。 */
  private void dropIndexIfExists(String tableName, String indexName) {
    Boolean exists = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) > 0 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND INDEX_NAME = ?",
        Boolean.class,
        tableName,
        indexName
    );
    if (!Boolean.TRUE.equals(exists)) {
      return;
    }
    try {
      jdbcTemplate.execute("ALTER TABLE " + tableName + " DROP INDEX " + indexName);
      log.info("Dropped legacy index {}.{}", tableName, indexName);
    } catch (Exception exception) {
      log.warn("Failed to drop index {}.{}: {}", tableName, indexName, exception.getMessage());
    }
  }
}

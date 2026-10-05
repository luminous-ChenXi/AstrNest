package com.chenxi.astrnest.install;

import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/**
 * 安装向导的写操作实现：安装数据库表结构、创建初始管理员、写入完成标记。
 *
 * <p>全部使用纯 JDBC，避免依赖 JPA 实体在"表尚未建好"时的行为；幂等性由
 * install-schema.sql（CREATE TABLE IF NOT EXISTS / WHERE NOT EXISTS 种子）与
 * 逐语句"已存在"错误码容错共同保证。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InstallSetupService {

  /** MySQL"对象已存在"类错误码：重复执行安装脚本时按"跳过"处理 */
  private static final Set<Integer> ALREADY_EXISTS_ERROR_CODES = Set.of(
      1050, // ER_TABLE_EXISTS_ERROR
      1051, // ER_BAD_TABLE_ERROR（DROP IF EXISTS 场景）
      1060, // ER_DUP_FIELDNAME
      1061, // ER_DUP_KEYNAME
      1062, // ER_DUP_ENTRY
      1091  // ER_CANT_DROP_FIELD_OR_KEY
  );

  private static final DateTimeFormatter TIMESTAMP_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  private final JdbcTemplate jdbcTemplate;
  private final PasswordEncoder passwordEncoder;
  private final PlatformTransactionManager transactionManager;
  private final InstallLockService installLockService;

  // ==================== 步骤 2：安装数据库 ====================

  /**
   * 执行 classpath:db/install-schema.sql（纯 schema 版，见文件头注释）。
   * 幂等：重复执行不破坏已有数据。
   */
  public InstallDatabaseResponse installDatabase() {
    String script;
    try {
      script = new ClassPathResource("db/install-schema.sql")
          .getContentAsString(StandardCharsets.UTF_8);
    } catch (Exception exception) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
          "读取安装脚本 db/install-schema.sql 失败：" + exception.getMessage());
    }

    List<String> statements = splitStatements(script);
    int executed = 0;
    int tablesCreated = 0;
    int skipped = 0;
    List<String> errors = new ArrayList<>();
    for (String statement : statements) {
      try {
        jdbcTemplate.execute(statement);
        executed++;
        String head = statement.trim().toUpperCase(Locale.ROOT);
        if (head.startsWith("CREATE TABLE")) {
          tablesCreated++;
        }
      } catch (Exception exception) {
        if (isAlreadyExistsError(exception)) {
          skipped++;
        } else {
          String prefix = statement.trim().replaceAll("\\s+", " ");
          if (prefix.length() > 120) {
            prefix = prefix.substring(0, 120) + "...";
          }
          errors.add(prefix + " => " + rootMessage(exception));
        }
      }
    }
    if (!errors.isEmpty()) {
      log.warn("Install schema finished with {} error(s): {}", errors.size(), errors);
    } else {
      log.info("Install schema finished: executed={}, tablesCreated={}, skipped={}", executed, tablesCreated, skipped);
    }
    return new InstallDatabaseResponse(errors.isEmpty(), executed, tablesCreated, skipped, errors);
  }

  // ==================== 步骤 3：创建初始管理员 ====================

  /**
   * 创建初始管理员（角色 ADMIN，复用/补齐 roles 表中的 ADMIN 行）。
   * 默认配额与邮箱注册的管理员语义一致：daily_upload_limit / storage_quota_mb 均为 NULL（不限制）。
   */
  public InstallAdminResponse createAdmin(InstallAdminRequest request) {
    String username = trimToNull(request.username());
    String email = InstallStatusService.normalizeLower(trimToNull(request.email()));
    String password = request.password() == null ? "" : request.password();
    String displayName = trimToNull(request.displayName());

    validateUsername(username);
    validateEmail(email);
    validatePassword(password);
    if (displayName == null) {
      displayName = username;
    }

    TransactionTemplate template = new TransactionTemplate(transactionManager);
    final String finalUsername = username;
    final String finalEmail = email;
    final String finalDisplayName = displayName;
    return template.execute(status -> {
      Long usernameCount = jdbcTemplate.queryForObject(
          "SELECT COUNT(*) FROM users WHERE username = ?", Long.class, finalUsername);
      if (usernameCount != null && usernameCount > 0) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "用户名已存在");
      }
      Long emailCount = jdbcTemplate.queryForObject(
          "SELECT COUNT(*) FROM users WHERE email = ?", Long.class, finalEmail);
      if (emailCount != null && emailCount > 0) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "该邮箱已注册");
      }

      Long adminRoleId = queryRoleId("ADMIN");
      if (adminRoleId == null) {
        // 复用 AdminAccountInitializer 建好的 ADMIN 角色行；若缺失（如 EMPTY 库由 Hibernate 建表）则补齐
        jdbcTemplate.update("INSERT INTO roles (name, description) VALUES (?, ?)", "ADMIN", "超级管理员");
        adminRoleId = queryRoleId("ADMIN");
        if (adminRoleId == null) {
          throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "缺少 ADMIN 角色");
        }
      }

      try {
        jdbcTemplate.update(
            "INSERT INTO users (username, password, nickname, email, active, email_verified, daily_upload_limit, "
                + "storage_quota_mb, identity_source, created_at) "
                + "VALUES (?, ?, ?, ?, 1, 1, NULL, NULL, 'local', CURRENT_TIMESTAMP)",
            finalUsername, passwordEncoder.encode(password), finalDisplayName, finalEmail);
      } catch (DuplicateKeyException exception) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "用户名或邮箱已存在");
      }
      Long userId = jdbcTemplate.queryForObject(
          "SELECT id FROM users WHERE username = ?", Long.class, finalUsername);
      jdbcTemplate.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", userId, adminRoleId);

      log.info("Install wizard: initial admin '{}' created", finalUsername);
      return new InstallAdminResponse(finalUsername, finalEmail, finalDisplayName, "ADMIN");
    });
  }

  // ==================== 步骤 4：完成安装 ====================

  /**
   * 写入完成标记（install_state.installed_at）；重复执行不覆盖原时间。
   * 同时采集安装汇总（管理员账号 / 站点开关 / SMTP 状态）随响应返回，供完成页展示。
   */
  public InstallFinishResponse finish() {
    jdbcTemplate.execute(
        "CREATE TABLE IF NOT EXISTS install_state ("
            + "id BIGINT PRIMARY KEY, "
            + "installed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP"
            + ") ENGINE = InnoDB DEFAULT CHARSET = utf8mb4");
    jdbcTemplate.update("INSERT IGNORE INTO install_state (id) VALUES (1)");
    Timestamp installedAt = jdbcTemplate.queryForObject(
        "SELECT installed_at FROM install_state WHERE id = 1", Timestamp.class);
    LocalDateTime time = installedAt == null ? LocalDateTime.now() : installedAt.toLocalDateTime();
    // 四重防护之 lock 文件：DB 标记之外的独立防重装信号（写入失败不阻断收尾）
    installLockService.writeLock();
    return new InstallFinishResponse(true, time.format(TIMESTAMP_FORMATTER), "安装完成", buildSummary());
  }

  /** 完成页汇总：初始管理员账号摘要 + 站点开关 + SMTP 状态（纯 JDBC + 全兜底，绝不抛异常阻断收尾）。 */
  private InstallFinishResponse.InstallSummary buildSummary() {
    String adminUsername = null;
    String adminEmail = null;
    try {
      List<String> row = jdbcTemplate.query(
          "SELECT u.username, u.email FROM users u "
              + "JOIN user_roles ur ON ur.user_id = u.id "
              + "JOIN roles r ON r.id = ur.role_id "
              + "WHERE r.name = 'ADMIN' ORDER BY u.id ASC LIMIT 1",
          (rs, num) -> rs.getString(1) + "\n" + rs.getString(2));
      if (!row.isEmpty()) {
        String[] parts = row.get(0).split("\n", -1);
        adminUsername = parts[0];
        adminEmail = parts.length > 1 ? parts[1] : null;
      }
    } catch (Exception exception) {
      log.debug("Install summary: admin probe failed: {}", exception.getMessage());
    }

    boolean emailVerifyRequired = false;
    boolean totpRequired = false;
    try {
      List<boolean[]> flags = jdbcTemplate.query(
          "SELECT registration_email_verify_required, login_totp_required FROM system_config WHERE id = 1",
          (rs, num) -> new boolean[] {rs.getBoolean(1), rs.getBoolean(2)});
      if (!flags.isEmpty()) {
        emailVerifyRequired = flags.get(0)[0];
        totpRequired = flags.get(0)[1];
      }
    } catch (Exception exception) {
      log.debug("Install summary: switches probe failed: {}", exception.getMessage());
    }

    boolean smtpConfigured = false;
    try {
      Integer count = jdbcTemplate.queryForObject(
          "SELECT COUNT(*) FROM chenxi_mail_config WHERE id = 1 AND enabled = 1 "
              + "AND smtp_password IS NOT NULL AND smtp_password <> '' AND smtp_password <> 'CHANGE_ME'",
          Integer.class);
      smtpConfigured = count != null && count > 0;
    } catch (Exception exception) {
      log.debug("Install summary: smtp probe failed: {}", exception.getMessage());
    }
    return new InstallFinishResponse.InstallSummary(
        adminUsername, adminEmail, emailVerifyRequired, totpRequired, smtpConfigured);
  }

  // ==================== 校验（风格与 ChenxiAuthService / RegisterAccountRequest 对齐） ====================

  private void validateUsername(String username) {
    if (username == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "用户名不能为空");
    }
    if (!username.matches("^[A-Za-z0-9_.-]{4,32}$")) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "用户名需 3-32 位，仅允许字母、数字、下划线、点、短横线");
    }
  }

  private void validateEmail(String email) {
    if (!StringUtils.hasText(email)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "邮箱不能为空");
    }
    if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$") || email.length() > 180) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "邮箱格式不正确");
    }
  }

  private void validatePassword(String password) {
    if (password.length() < 8 || password.length() > 64) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "密码长度需在 8-64 位之间");
    }
    boolean hasLetter = password.matches(".*[A-Za-z].*");
    boolean hasDigit = password.matches(".*\\d.*");
    if (!hasLetter || !hasDigit) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "密码需同时包含字母和数字");
    }
  }

  // ==================== 脚本解析与执行工具 ====================

  /**
   * 解析 SQL 文件为可逐条执行的语句列表。
   * install-schema.sql 仅使用整行 -- 注释与单引号字符串（无 DELIMITER/存储过程/触发器），
   * 因此按"引号外分号"切分即可；语句间容错由调用方处理。
   */
  private List<String> splitStatements(String script) {
    StringBuilder cleaned = new StringBuilder(script.length());
    for (String line : script.split("\n", -1)) {
      if (line.trim().startsWith("--") || line.trim().startsWith("#")) {
        continue;
      }
      cleaned.append(line).append('\n');
    }
    List<String> statements = new ArrayList<>();
    StringBuilder current = new StringBuilder();
    boolean inString = false;
    for (int i = 0; i < cleaned.length(); i++) {
      char c = cleaned.charAt(i);
      if (c == '\'') {
        inString = !inString;
      }
      if (c == ';' && !inString) {
        addIfMeaningful(statements, current.toString());
        current.setLength(0);
      } else {
        current.append(c);
      }
    }
    addIfMeaningful(statements, current.toString());
    return statements;
  }

  private void addIfMeaningful(List<String> statements, String raw) {
    String trimmed = raw.trim();
    if (!trimmed.isEmpty()) {
      statements.add(trimmed);
    }
  }

  private boolean isAlreadyExistsError(Exception exception) {
    Throwable cause = exception;
    while (cause != null) {
      if (cause instanceof SQLException sqlException) {
        if (ALREADY_EXISTS_ERROR_CODES.contains(sqlException.getErrorCode())) {
          return true;
        }
      }
      cause = cause.getCause();
    }
    return false;
  }

  private Long queryRoleId(String roleName) {
    List<Long> ids = jdbcTemplate.queryForList("SELECT id FROM roles WHERE name = ?", Long.class, roleName);
    return ids.isEmpty() ? null : ids.get(0);
  }

  private String rootMessage(Throwable throwable) {
    Throwable current = throwable;
    while (current.getCause() != null && current.getCause() != current) {
      current = current.getCause();
    }
    String message = current.getMessage();
    if (!StringUtils.hasText(message)) {
      message = current.getClass().getSimpleName();
    }
    return message.length() > 300 ? message.substring(0, 300) + "..." : message;
  }

  private String trimToNull(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}

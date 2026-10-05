package com.chenxi.astrnest.install;

import com.chenxi.astrnest.chenxi.mail.ChenxiMailConfig;
import com.chenxi.astrnest.chenxi.mail.ChenxiMailConfigService;
import com.chenxi.astrnest.storage.StorageProperties;
import com.chenxi.astrnest.storage.StorageStrategy;
import com.chenxi.astrnest.upload.media.VideoThumbnailProperties;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 安装状态与环境检测（WordPress 式安装向导的后端核心）。
 *
 * <p><b>installed 的唯一定义</b>：users 表存在且用户数大于 0。三态 schemaState：
 * NOT_INSTALLED（users 表不存在）/ EMPTY（表在但 0 用户）/ INSTALLED（有用户）。</p>
 *
 * <p>全部探测使用纯 JDBC（javax.sql.DataSource / JdbcTemplate）查询 information_schema
 * 与计数，任何异常（数据库挂了 / 表不存在 / 版本不支持）都就地兜底为"未安装 + 检查项失败"，
 * <b>绝不触发 JPA</b>（schema 未安装时 JPA 查询会直接抛异常）。结果缓存数秒防抖，
 * 避免每次请求都打数据库。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InstallStatusService {

  public static final String STATE_NOT_INSTALLED = "NOT_INSTALLED";
  public static final String STATE_EMPTY = "EMPTY";
  public static final String STATE_INSTALLED = "INSTALLED";

  /** 快照缓存时长：兼顾防抖与"重新检测"的即时性 */
  private static final long CACHE_MILLIS = 3500L;
  /** 核心表概览：不追求穷举，给出数量级信息即可 */
  private static final List<String> CORE_TABLES = List.of("users", "roles", "upload_records", "albums");
  /** 推荐的最低 MySQL 版本 */
  private static final int RECOMMENDED_MYSQL_MAJOR = 8;

  private final JdbcTemplate jdbcTemplate;
  private final StorageProperties storageProperties;
  private final VideoThumbnailProperties videoThumbnailProperties;
  private final InstallLockService installLockService;
  private final ChenxiMailConfigService mailConfigService;

  /** 缓存条目：快照与采集时间绑定为一个不可变对象，保证读侧原子可见 */
  private volatile CacheEntry cacheEntry;

  /** 获取安装状态快照（带 3.5 秒缓存；数据库异常时返回"未安装"快照而非抛异常） */
  public InstallSnapshot getSnapshot() {
    CacheEntry entry = cacheEntry;
    if (entry != null && System.currentTimeMillis() - entry.atMillis() < CACHE_MILLIS) {
      return entry.snapshot();
    }
    synchronized (this) {
      entry = cacheEntry;
      if (entry != null && System.currentTimeMillis() - entry.atMillis() < CACHE_MILLIS) {
        return entry.snapshot();
      }
      InstallSnapshot snapshot = probe();
      cacheEntry = new CacheEntry(snapshot, System.currentTimeMillis());
      return snapshot;
    }
  }

  /** 是否已完成安装（users 表存在且用户数大于 0） */
  public boolean isInstalled() {
    return getSnapshot().installed();
  }

  /**
   * 清除缓存快照：安装向导的写操作（建表/建管理员/完成标记）成功后必须调用，
   * 否则缓存窗口内的后续请求会读到过期的安装状态。
   */
  public void evictCache() {
    cacheEntry = null;
  }

  private record CacheEntry(InstallSnapshot snapshot, long atMillis) {
  }

  /**
   * 向导是否已完成（install_state 表中已写入 installed_at 完成标记）。
   * 与 isInstalled 的区别：创建管理员后 installed 即为 true，但 finish 标记是
   * 「向导已走完」的判据——finish 端点用它在 403（已完成）与放行之间做区分。
   * 纯 JDBC + 全兜底：表不存在/库异常时返回 false。
   */
  public boolean isFinished() {
    try {
      Boolean tableExists = jdbcTemplate.queryForObject(
          "SELECT COUNT(*) > 0 FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'install_state'",
          Boolean.class
      );
      if (!Boolean.TRUE.equals(tableExists)) {
        return false;
      }
      Integer rows = jdbcTemplate.queryForObject(
          "SELECT COUNT(*) FROM install_state WHERE id = 1", Integer.class);
      return rows != null && rows > 0;
    } catch (Exception exception) {
      log.debug("isFinished probe failed: {}", exception.getMessage());
      return false;
    }
  }

  /**
   * 防重装锁是否命中：DB 完成标记或 install.lock 文件任一存在即命中。
   * 命中后安装向导的写端点一律 403（四重防护中的 API 层）。
   */
  public boolean isLocked() {
    return isFinished() || installLockService.isLocked();
  }

  /** 构建完整状态响应（含环境检测项列表） */
  public InstallStatusResponse buildStatus() {
    InstallSnapshot snapshot = getSnapshot();
    return new InstallStatusResponse(
        snapshot.installed(), snapshot.schemaState(), isFinished(), isLocked(), buildChecks(snapshot));
  }

  /** 构建环境检测项列表（数据库 / 表结构 / 存储目录 / Java / ffmpeg / SMTP） */
  public List<InstallCheckItem> buildChecks(InstallSnapshot snapshot) {
    List<InstallCheckItem> checks = new ArrayList<>();
    checks.add(databaseCheck(snapshot));
    checks.add(schemaCheck(snapshot));
    checks.add(storageCheck());
    checks.add(javaCheck());
    checks.add(ffmpegCheck());
    checks.add(smtpCheck(snapshot));
    return checks;
  }

  // ==================== 状态探测（纯 JDBC，全部兜底） ====================

  private InstallSnapshot probe() {
    String dbVersion = null;
    String dbError = null;
    try {
      // 可达性探测用全方言通用语句：VERSION() 是 MySQL 函数，H2 没有（测试内存库会误判为不可达）
      jdbcTemplate.queryForObject("SELECT 1", Integer.class);
      try {
        dbVersion = jdbcTemplate.queryForObject("SELECT VERSION()", String.class);
      } catch (Exception mysqlOnly) {
        try {
          dbVersion = "H2 " + jdbcTemplate.queryForObject("SELECT H2VERSION()", String.class);
        } catch (Exception ignored) {
          dbVersion = null;
        }
      }
    } catch (Exception exception) {
      dbError = rootMessage(exception);
    }
    if (dbError != null) {
      // 数据库不可达：不抛异常，返回"未安装 + 连接失败"
      log.debug("Install probe: database unreachable: {}", dbError);
      return new InstallSnapshot(false, null, dbError, STATE_NOT_INSTALLED, false, 0, Map.of(), 0);
    }

    boolean usersTableExists = tableExists("users");
    long userCount = 0;
    if (usersTableExists) {
      try {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Long.class);
        userCount = count == null ? 0 : count;
      } catch (Exception exception) {
        // 表存在但计数失败（权限/损坏等）：按"表不可用"处理，避免误判为已安装
        log.warn("Install probe: failed to count users: {}", rootMessage(exception));
        usersTableExists = false;
      }
    }

    String schemaState = !usersTableExists ? STATE_NOT_INSTALLED
        : (userCount > 0 ? STATE_INSTALLED : STATE_EMPTY);
    boolean installed = STATE_INSTALLED.equals(schemaState);

    Map<String, Boolean> coreTables = new LinkedHashMap<>();
    for (String table : CORE_TABLES) {
      coreTables.put(table, tableExists(table));
    }
    return new InstallSnapshot(true, dbVersion, null, schemaState, installed, userCount, coreTables, countTables());
  }

  private boolean tableExists(String tableName) {
    try {
      // TABLE_SCHEMA = 'PUBLIC' 兼容 H2（测试内存库）：H2 的 information_schema 里
      // 一律是 PUBLIC schema，DATABASE() 永远不匹配；MySQL 无 PUBLIC schema，双条件互不干扰
      Boolean exists = jdbcTemplate.queryForObject(
          "SELECT COUNT(*) > 0 FROM information_schema.TABLES "
              + "WHERE UPPER(TABLE_NAME) = UPPER(?) AND (TABLE_SCHEMA = DATABASE() OR TABLE_SCHEMA = 'PUBLIC')",
          Boolean.class,
          tableName
      );
      return Boolean.TRUE.equals(exists);
    } catch (Exception exception) {
      log.debug("Install probe: tableExists({}) failed: {}", tableName, rootMessage(exception));
      return false;
    }
  }

  private int countTables() {
    try {
      Integer count = jdbcTemplate.queryForObject(
          "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() OR TABLE_SCHEMA = 'PUBLIC'",
          Integer.class
      );
      return count == null ? 0 : count;
    } catch (Exception exception) {
      return 0;
    }
  }

  // ==================== 环境检测项 ====================

  private InstallCheckItem databaseCheck(InstallSnapshot snapshot) {
    if (!snapshot.dbReachable()) {
      return new InstallCheckItem("database", "数据库连接", false, false,
          "无法连接数据库：" + (StringUtils.hasText(snapshot.dbError()) ? snapshot.dbError() : "未知错误")
              + "。请确认 MySQL 已启动，并检查 ASTRNEST_DB_URL / ASTRNEST_DB_USERNAME / ASTRNEST_DB_PASSWORD 配置。");
    }
    Integer major = parseMajorVersion(snapshot.dbVersion());
    if (major != null && major < RECOMMENDED_MYSQL_MAJOR) {
      return new InstallCheckItem("database", "数据库连接", true, true,
          "连接成功，MySQL " + snapshot.dbVersion()
              + "（低于推荐的 MySQL 8.0，部分特性如 JSON/FULLTEXT 可能异常，建议升级）");
    }
    return new InstallCheckItem("database", "数据库连接", true, false,
        "连接成功，MySQL " + snapshot.dbVersion() + "（推荐 MySQL 8.0 及以上）");
  }

  private InstallCheckItem schemaCheck(InstallSnapshot snapshot) {
    List<String> missing = new ArrayList<>();
    snapshot.coreTables().forEach((table, exists) -> {
      if (!Boolean.TRUE.equals(exists)) {
        missing.add(table);
      }
    });
    String overview = "核心表概览：users/roles/upload_records/albums 共 "
        + (CORE_TABLES.size() - missing.size()) + "/" + CORE_TABLES.size() + " 张存在";
    return switch (snapshot.schemaState()) {
      case STATE_INSTALLED -> new InstallCheckItem("schema", "数据库表结构", true, false,
          "已安装：" + snapshot.userCount() + " 个用户，当前库共 " + snapshot.totalTables() + " 张表。" + overview);
      case STATE_EMPTY -> new InstallCheckItem("schema", "数据库表结构", true, true,
          "数据表已就绪但还没有任何用户（共 " + snapshot.totalTables() + " 张表），可继续创建初始管理员。" + overview);
      default -> new InstallCheckItem("schema", "数据库表结构", false, false,
          "尚未安装数据表（当前库共 " + snapshot.totalTables() + " 张表），可执行下一步「安装数据库」自动建表。" + overview);
    };
  }

  private InstallCheckItem storageCheck() {
    StorageStrategy strategy = storageProperties.getStrategy();
    if (strategy != StorageStrategy.LOCAL) {
      return new InstallCheckItem("storage", "本地存储目录", true, false,
          "当前存储策略为 " + strategy.name() + "，使用云端对象存储，跳过本地目录检测。");
    }
    Path root = storageProperties.getLocal().resolvedRoot();
    try {
      Files.createDirectories(root);
      Path probe = root.resolve(".astrnest-install-write-test-" + System.currentTimeMillis() + ".tmp");
      Files.writeString(probe, "ok");
      Files.deleteIfExists(probe);
      return new InstallCheckItem("storage", "本地存储目录", true, false,
          "目录可写：" + root.toAbsolutePath());
    } catch (Exception exception) {
      return new InstallCheckItem("storage", "本地存储目录", false, true,
          "目录不可写：" + root.toAbsolutePath() + "（" + rootMessage(exception)
              + "）。请检查 astrnest.storage.local.root（ASTRNEST_STORAGE_ROOT）指向的目录权限；"
              + "不修复会影响本地上传，但不阻断安装，也可在管理后台切换云端存储。");
    }
  }

  private InstallCheckItem javaCheck() {
    String version = System.getProperty("java.version");
    String vendor = System.getProperty("java.vendor");
    return new InstallCheckItem("java", "Java 运行时", true, false,
        "Java " + version + "（" + vendor + "）。Spring Boot 3.4 要求 Java 17+，推荐 Java 21。");
  }

  private InstallCheckItem ffmpegCheck() {
    // 探测方式与 VideoThumbnailService 一致：直接执行配置的 ffmpeg 命令
    String ffmpegPath = videoThumbnailProperties.getFfmpegPath();
    if (!videoThumbnailProperties.isEnabled()) {
      return new InstallCheckItem("ffmpeg", "FFmpeg（视频缩略图）", true, false,
          "视频缩略图功能已在配置中关闭（astrnest.video-thumbnail.enabled=false），跳过检测。");
    }
    try {
      ProcessBuilder builder = new ProcessBuilder(ffmpegPath, "-version");
      builder.redirectErrorStream(true);
      Process process = builder.start();
      String firstLine = null;
      try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()))) {
        firstLine = reader.readLine();
      }
      boolean finished = process.waitFor(5, TimeUnit.SECONDS);
      if (!finished) {
        process.destroyForcibly();
      }
      if (finished && process.exitValue() == 0) {
        return new InstallCheckItem("ffmpeg", "FFmpeg（视频缩略图）", true, false,
            "已检测到 ffmpeg：" + (StringUtils.hasText(firstLine) ? firstLine : ffmpegPath));
      }
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
    } catch (Exception exception) {
      log.debug("ffmpeg probe failed: {}", rootMessage(exception));
    }
    return new InstallCheckItem("ffmpeg", "FFmpeg（视频缩略图）", false, true,
        "未检测到可用的 ffmpeg（" + ffmpegPath + "）。仅影响视频缩略图生成，不影响安装、图片上传与站点运行；"
            + "可安装 ffmpeg 或通过 ASTRNEST_VIDEO_THUMBNAIL_FFMPEG 指定完整路径后重启。");
  }

  /**
   * SMTP 邮件服务自检（只检测展示，不阻塞安装）：
   * 注册邮箱验证、密码找回等邮件能力依赖它，未配置时给出管理后台入口提示。
   * 数据库尚未初始化（业务表不存在）时跳过，避免 JPA 触发 500。
   */
  private InstallCheckItem smtpCheck(InstallSnapshot snapshot) {
    if (InstallStatusService.STATE_NOT_INSTALLED.equals(snapshot.schemaState())) {
      return new InstallCheckItem("smtp", "SMTP 邮件服务", false, true,
          "数据库尚未初始化，跳过 SMTP 检测。安装完成后可在管理后台「邮件设置」中配置发件邮箱。");
    }
    try {
      ChenxiMailConfig config = mailConfigService.getOrDefault();
      boolean enabled = config.isEnabled();
      boolean filled = StringUtils.hasText(config.getSmtpHost())
          && StringUtils.hasText(config.getFromEmail())
          && StringUtils.hasText(config.getSmtpPassword())
          && !"CHANGE_ME".equals(config.getSmtpPassword())
          && !"smtp.example.com".equals(config.getSmtpHost());
      if (enabled && filled) {
        return new InstallCheckItem("smtp", "SMTP 邮件服务", true, false,
            "已配置：" + config.getSmtpHost() + "（发件人 " + config.getFromEmail() + "）。"
                + "注册邮箱验证、密码找回等邮件能力可用。");
      }
      return new InstallCheckItem("smtp", "SMTP 邮件服务", false, true,
          "尚未配置" + (enabled ? "完整" : "或未启用") + "。不影响安装与本站运行，但注册邮箱验证、密码找回等"
              + "依赖邮件的功能不可用；请在管理后台「邮件设置」中配置 SMTP 后开启。");
    } catch (Exception exception) {
      return new InstallCheckItem("smtp", "SMTP 邮件服务", false, true,
          "暂时无法读取 SMTP 配置（" + rootMessage(exception) + "）。不影响安装，可稍后在管理后台配置。");
    }
  }

  // ==================== 工具 ====================

  private Integer parseMajorVersion(String version) {
    if (!StringUtils.hasText(version)) {
      return null;
    }
    try {
      String normalized = version.trim();
      if (normalized.startsWith("mysql-")) { // MariaDB 兼容串如 mysql-5.5.5-10.4.x
        normalized = normalized.substring("mysql-".length());
      }
      String[] segments = normalized.split("\\.");
      return Integer.parseInt(segments[0]);
    } catch (Exception exception) {
      return null;
    }
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

  static String normalizeLower(String value) {
    return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
  }
}

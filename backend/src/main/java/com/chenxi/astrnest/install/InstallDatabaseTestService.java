package com.chenxi.astrnest.install;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 安装向导「数据库配置」步骤的连接测试：用表单参数直连目标 MySQL，即时反馈可用性。
 *
 * <p>设计要点：</p>
 * <ul>
 *   <li>与运行时 {@code JdbcTemplate} 完全隔离：使用 {@link DriverManager} 建立一次性短连接
 *       （连接/读库超时各 5 秒），测试完立即关闭，不污染连接池；</li>
 *   <li>失败时给出分类原因（连接拒绝 / 认证失败 / 库不存在 / 其他），库不存在且调用方允许时
 *       会尝试 {@code CREATE DATABASE IF NOT EXISTS}（需要账号具备建库权限）；</li>
 *   <li>成功时回显 MySQL 版本与服务器/目标库字符集；</li>
 *   <li>运行时连接（spring.datasource.url）一并回显并比对：表单与运行时不一致时提示部署者
 *       修改 ASTRNEST_DB_URL 后重启，避免「测试通过的是 A 库、初始化写入的是 B 库」。</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InstallDatabaseTestService {

  private static final int CONNECT_TIMEOUT_MILLIS = 5000;
  private static final int SOCKET_TIMEOUT_MILLIS = 8000;

  /** 数据库名称白名单：杜绝借建库语句注入任意 SQL */
  private static final String DATABASE_NAME_PATTERN = "^[A-Za-z0-9_-]{1,64}$";

  private final Environment environment;

  /** 用表单参数测试连接；createIfMissing 仅在「库不存在」时生效。 */
  public InstallDbTestResponse test(InstallDbTestRequest request) {
    String host = normalizeHost(request);
    int port = request.port() == null ? 3306 : request.port();
    String database = trimToNull(request.databaseName());
    String username = trimToNull(request.username());
    String password = request.password() == null ? "" : request.password();
    String runtimeUrl = environment.getProperty("spring.datasource.url");

    if (database == null) {
      return fail("请填写数据库名称", null, runtimeUrl);
    }
    if (!database.matches(DATABASE_NAME_PATTERN)) {
      return fail("数据库名称仅允许字母、数字、下划线与短横线（1-64 位）", null, runtimeUrl);
    }
    if (!StringUtils.hasText(username)) {
      return fail("请填写数据库用户名", null, runtimeUrl);
    }
    String url = buildUrl(host, port, database);

    try (Connection connection = openConnection(url, username, password)) {
      String version = queryScalar(connection, "SELECT VERSION()");
      String charsetServer = queryScalar(connection, "SELECT @@character_set_server");
      String charsetDatabase = queryScalar(connection, "SELECT @@character_set_database");
      return new InstallDbTestResponse(true,
          "连接成功：MySQL " + version + "，服务器字符集 " + charsetServer + "，目标库字符集 " + charsetDatabase,
          version, charsetServer, charsetDatabase, false, null, runtimeUrl, matchesRuntime(runtimeUrl, host, port, database));
    } catch (SQLException exception) {
      String errorCode = classify(exception);
      if ("DB_MISSING".equals(errorCode) && request.createIfMissing()) {
        return tryCreateDatabaseThenRetest(request, host, port, database, username, password, runtimeUrl);
      }
      return fail(describe(errorCode, exception), errorCode, runtimeUrl);
    } catch (Exception exception) {
      log.debug("Install DB test failed: {}", exception.getMessage());
      return fail("连接失败：" + rootMessage(exception), "UNKNOWN", runtimeUrl);
    }
  }

  /** 库不存在且允许创建：先连服务器（不带库名）执行 CREATE DATABASE IF NOT EXISTS，再重测。 */
  private InstallDbTestResponse tryCreateDatabaseThenRetest(InstallDbTestRequest request, String host, int port,
      String database, String username, String password, String runtimeUrl) {
    String serverUrl = buildUrl(host, port, null);
    try (Connection connection = openConnection(serverUrl, username, password);
        Statement statement = connection.createStatement()) {
      statement.execute("CREATE DATABASE IF NOT EXISTS `" + database + "` "
          + "CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci");
      log.info("Install wizard: database '{}' created on {}:{}", database, host, port);
    } catch (SQLException exception) {
      // 常见根因：账号无 CREATE 权限
      return new InstallDbTestResponse(false,
          "数据库「" + database + "」不存在，且自动创建失败（" + rootMessage(exception)
              + "）。请确认该账号具备建库权限，或由管理员手工建库后再测试。",
          null, null, null, false, "CREATE_FAILED", runtimeUrl, false);
    } catch (Exception exception) {
      return new InstallDbTestResponse(false,
          "数据库「" + database + "」不存在，且自动创建失败（" + rootMessage(exception) + "）。",
          null, null, null, false, "CREATE_FAILED", runtimeUrl, false);
    }
    try (Connection connection = openConnection(buildUrl(host, port, database), username, password)) {
      String version = queryScalar(connection, "SELECT VERSION()");
      String charsetServer = queryScalar(connection, "SELECT @@character_set_server");
      String charsetDatabase = queryScalar(connection, "SELECT @@character_set_database");
      return new InstallDbTestResponse(true,
          "数据库「" + database + "」已创建并连接成功：MySQL " + version
              + "，服务器字符集 " + charsetServer + "，目标库字符集 " + charsetDatabase,
          version, charsetServer, charsetDatabase, true, null, runtimeUrl,
          matchesRuntime(runtimeUrl, host, port, database));
    } catch (Exception exception) {
      return fail("数据库已创建但再次连接失败：" + rootMessage(exception), "UNKNOWN", runtimeUrl);
    }
  }

  // ==================== 连接与诊断 ====================

  private Connection openConnection(String url, String username, String password) throws SQLException {
    Properties props = new Properties();
    props.setProperty("user", username);
    props.setProperty("password", password);
    props.setProperty("connectTimeout", String.valueOf(CONNECT_TIMEOUT_MILLIS));
    props.setProperty("socketTimeout", String.valueOf(SOCKET_TIMEOUT_MILLIS));
    props.setProperty("useSSL", "false");
    props.setProperty("allowPublicKeyRetrieval", "true");
    props.setProperty("characterEncoding", "UTF-8");
    return DriverManager.getConnection(url, props);
  }

  private String buildUrl(String host, int port, String database) {
    StringBuilder url = new StringBuilder("jdbc:mysql://").append(host).append(':').append(port).append('/');
    if (StringUtils.hasText(database)) {
      url.append(database);
    }
    url.append("?connectTimeout=").append(CONNECT_TIMEOUT_MILLIS)
        .append("&socketTimeout=").append(SOCKET_TIMEOUT_MILLIS);
    return url.toString();
  }

  /** 把 JDBC 异常归类为人可读的失败原因。 */
  private String classify(SQLException exception) {
    String message = String.valueOf(exception.getMessage()).toLowerCase();
    Throwable root = rootCause(exception);
    String rootMessage = String.valueOf(root.getMessage()).toLowerCase();
    if (message.contains("access denied") || rootMessage.contains("access denied")) {
      return "AUTH_FAILED";
    }
    if (message.contains("unknown database") || rootMessage.contains("unknown database")) {
      return "DB_MISSING";
    }
    if (message.contains("communications link failure") || message.contains("connection refused")
        || rootMessage.contains("connection refused") || rootMessage.contains("connect timed out")
        || message.contains("connect timed out") || message.contains("handshake with database failed")) {
      return "CONNECTION_REFUSED";
    }
    return "UNKNOWN";
  }

  private String describe(String errorCode, SQLException exception) {
    return switch (errorCode) {
      case "CONNECTION_REFUSED" -> "无法连接到数据库服务器（连接被拒绝或超时）。请确认主机与端口正确、MySQL 已启动、"
          + "防火墙/安全组放行该端口；远程库还需确认账号允许来自应用主机的连接。";
      case "AUTH_FAILED" -> "认证失败：用户名或密码错误。请核对数据库账号密码；远程库还需确认该账号的主机白名单（如 'user'@'%'）。";
      case "DB_MISSING" -> "连接成功但数据库不存在。请核对数据库名称，或勾选「尝试创建数据库」（需要账号具备建库权限）。";
      default -> "连接失败：" + rootMessage(exception);
    };
  }

  private InstallDbTestResponse fail(String message, String errorCode, String runtimeUrl) {
    return new InstallDbTestResponse(false, message, null, null, null, false, errorCode, runtimeUrl, false);
  }

  // ==================== 运行时连接比对 ====================

  /**
   * 从 spring.datasource.url 提取 host:port/db 与表单比对。
   * 仅用于提示信息（解析失败一律视为不一致，交由部署者自行确认），绝不抛异常。
   */
  private boolean matchesRuntime(String runtimeUrl, String host, int port, String database) {
    if (!StringUtils.hasText(runtimeUrl)) {
      return false;
    }
    var matcher = java.util.regex.Pattern
        .compile("jdbc:mysql://([^/:?]+):(\\d+)/([^?]+)")
        .matcher(runtimeUrl.trim());
    if (!matcher.find()) {
      return false;
    }
    return host.equals(matcher.group(1))
        && port == Integer.parseInt(matcher.group(2))
        && database.equals(matcher.group(3));
  }

  // ==================== 工具 ====================

  private String queryScalar(Connection connection, String sql) throws SQLException {
    try (Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery(sql)) {
      return resultSet.next() ? resultSet.getString(1) : null;
    }
  }

  private String normalizeHost(InstallDbTestRequest request) {
    if (StringUtils.hasText(request.host())) {
      return request.host().trim();
    }
    // location=local 时不填即视为本机
    return "remote".equalsIgnoreCase(request.location()) ? "" : "localhost";
  }

  private Throwable rootCause(Throwable throwable) {
    Throwable current = throwable;
    while (current.getCause() != null && current.getCause() != current) {
      current = current.getCause();
    }
    return current;
  }

  private String rootMessage(Throwable throwable) {
    Throwable current = rootCause(throwable);
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

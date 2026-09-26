package com.chenxi.astrnest.install;

import com.chenxi.astrnest.storage.StorageProperties;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * install.lock 等价防重装锁——安装向导「四重防护」之一：
 * <ol>
 *   <li><b>lock 文件</b>（本类）：finish 成功后在存储根目录写入 {@code install.lock}（仅含完成时间，无敏感信息）；</li>
 *   <li><b>DB 完成标记</b>：install_state 表（见 {@link InstallSetupService#finish()}）；</li>
 *   <li><b>API 403</b>：{@link InstallController} 对已锁定系统的全部向导写端点返回 403；</li>
 *   <li><b>前端跳转</b>：路由守卫据 /api/install/status 的 installed/locked 把已安装访问弹离 /install。</li>
 * </ol>
 *
 * <p>lock 文件是 DB 标记之外的<b>独立冗余信号</b>：即便数据库被清空或整体替换，只要锁文件还在，
 * 安装接口依旧拒绝——防止"清库后由陌生人重放安装接口抢占站点"。确需重装：停止后端 → 清空数据库 →
 * 删除 {@code storage/install.lock} → 重启后再走向导。</p>
 *
 * <p>文件写入失败（只读卷等）不阻断安装流程：降级为仅 DB 标记防护并打 WARN 日志。
 * 锁文件内容只有时间戳与固定标记，即使被静态托管意外公开也无害。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InstallLockService {

  private static final String LOCK_FILE_NAME = "install.lock";
  private static final String LOCK_MARKER = "astrnest-install-lock";
  private static final DateTimeFormatter TIMESTAMP_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  private final StorageProperties storageProperties;

  /** 系统是否已被锁死（锁文件存在）。与 DB 完成标记互为冗余，任一命中即视为向导已关闭。 */
  public boolean isLocked() {
    try {
      return Files.exists(lockFilePath());
    } catch (Exception exception) {
      log.debug("install.lock 探测失败，按未锁定处理：{}", exception.getMessage());
      return false;
    }
  }

  /** 写入锁文件（幂等：已存在时不覆盖原时间）。失败仅告警不抛异常，不阻断安装收尾。 */
  public synchronized void writeLock() {
    Path path = lockFilePath();
    try {
      if (Files.exists(path)) {
        return;
      }
      Files.createDirectories(path.getParent());
      String content = LOCK_MARKER + "\ninstalled_at: " + LocalDateTime.now().format(TIMESTAMP_FORMATTER) + "\n";
      Files.writeString(path, content, StandardCharsets.UTF_8);
      log.info("install.lock 已写入：{}", path.toAbsolutePath());
    } catch (Exception exception) {
      log.warn("install.lock 写入失败（不影响安装完成，仅缺少文件级防重装信号）：{}，原因：{}",
          path.toAbsolutePath(), exception.getMessage());
    }
  }

  /**
   * 锁文件路径：存储根目录（astrnest.storage.local.root，默认 ./storage/upload）的上一级，
   * 即默认 {@code ./storage/install.lock}——与上传目录分离，避免进入公开静态托管范围；
   * 无父目录时兜底到工作目录。
   */
  private Path lockFilePath() {
    Path root = storageProperties.getLocal().resolvedRoot().toAbsolutePath().normalize();
    Path base = root.getParent() != null ? root.getParent() : root;
    if (!StringUtils.hasText(base.toString())) {
      base = Path.of(".").toAbsolutePath().normalize();
    }
    return base.resolve(LOCK_FILE_NAME);
  }
}

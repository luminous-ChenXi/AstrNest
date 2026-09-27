package com.chenxi.astrnest.install;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * 安装向导「重置安装状态」：装库/收尾失败后清掉残留的完成标记，允许部署者从头再走向导。
 *
 * <p>清理范围（仅此两项，不动业务数据）：</p>
 * <ul>
 *   <li>DB 完成标记：清空 {@code install_state} 表（表不存在时跳过）；</li>
 *   <li>install.lock 锁文件：删除 {@code storage/install.lock}。</li>
 * </ul>
 *
 * <p>安全边界：仅「未完成站点」可调用——{@code users} 表不存在或没有任何用户
 * （即安装失败/回滚后的空壳）时才允许重置。已存在用户的站点一律 403，
 * 防止陌生人重放该接口把正常站点打回可重装状态。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InstallResetService {

  private final InstallStatusService installStatusService;
  private final InstallLockService installLockService;
  private final JdbcTemplate jdbcTemplate;

  public InstallResetResponse reset() {
    InstallSnapshot snapshot = installStatusService.getSnapshot();
    if (InstallStatusService.STATE_INSTALLED.equals(snapshot.schemaState())) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN,
          "站点已存在用户（安装已完成），禁止重置安装状态；如需重装请清空数据库并删除 storage/install.lock 后重启");
    }
    boolean lockCleared = installLockService.clearLock();
    boolean markerCleared = clearFinishMarker();
    installStatusService.evictCache();
    log.info("Install wizard: install state reset (lockCleared={}, markerCleared={})", lockCleared, markerCleared);
    return new InstallResetResponse(true,
        "安装状态已重置" + (lockCleared ? "，install.lock 已删除" : "") + (markerCleared ? "，DB 完成标记已清理" : "")
            + "。可重新执行安装向导。");
  }

  /** 清空 install_state 完成标记。表不存在（从未走到 finish）视为已清理。 */
  private boolean clearFinishMarker() {
    try {
      Boolean tableExists = jdbcTemplate.queryForObject(
          "SELECT COUNT(*) > 0 FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'install_state'",
          Boolean.class);
      if (!Boolean.TRUE.equals(tableExists)) {
        return false;
      }
      int removed = jdbcTemplate.update("DELETE FROM install_state");
      return removed > 0;
    } catch (Exception exception) {
      log.warn("Install reset: failed to clear install_state: {}", exception.getMessage());
      return false;
    }
  }
}

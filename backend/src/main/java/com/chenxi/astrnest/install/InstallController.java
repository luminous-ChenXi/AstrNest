package com.chenxi.astrnest.install;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * WordPress 式安装向导接口。
 *
 * <p>流程对齐统一规范：环境检测 → 数据库配置 → 初始化（建表）→ 站点配置 → 管理员创建 → 完成。
 * 其中"数据库配置"由部署环境（ASTRNEST_DB_URL/USERNAME/PASSWORD）承载，向导内只做连接确认。</p>
 *
 * <p>四重防护保证安全：</p>
 * <ul>
 *   <li>SecurityConfig：/api/install/** permitAll（匿名可探测状态）；</li>
 *   <li>InstallGuardFilter：未安装时拦截其余 /api/** 返回 503；</li>
 *   <li>本 Controller：防重装锁命中（install.lock 文件或完成标记）后所有写操作端点一律 403
 *       （防止装完后被重放调用），步骤顺序不满足时返回 409；</li>
 *   <li>前端路由守卫：已安装/已锁定时把 /install 访问弹离。</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/install")
@RequiredArgsConstructor
public class InstallController {

  private final InstallStatusService installStatusService;
  private final InstallSetupService installSetupService;
  private final InstallSiteConfigService installSiteConfigService;

  /** 任何时候都可访问（安装完成后也返回 installed/locked:true 供前端判断） */
  @GetMapping("/status")
  public InstallStatusResponse status() {
    return installStatusService.buildStatus();
  }

  /** 初始化数据库表结构（仅 schemaState != INSTALLED 时允许，幂等） */
  @PostMapping("/database")
  public InstallDatabaseResponse installDatabase() {
    ensureWizardUnlocked();
    if (InstallStatusService.STATE_INSTALLED.equals(installStatusService.getSnapshot().schemaState())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "数据库已包含 AstrNest 表结构，无需重复安装");
    }
    InstallDatabaseResponse response = installSetupService.installDatabase();
    installStatusService.evictCache();
    return response;
  }

  /** 保存站点初始配置（需已完成初始化建表；可多次调用，未提供的字段保持默认） */
  @PostMapping("/site-config")
  public InstallSiteConfigResponse saveSiteConfig(@Valid @RequestBody InstallSiteConfigRequest request) {
    ensureWizardUnlocked();
    String schemaState = installStatusService.getSnapshot().schemaState();
    if (InstallStatusService.STATE_NOT_INSTALLED.equals(schemaState)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "请先完成「初始化数据库」步骤");
    }
    installSiteConfigService.apply(request);
    return new InstallSiteConfigResponse(true, "站点配置已保存");
  }

  /** 创建初始管理员（仅 schemaState == EMPTY 时允许） */
  @PostMapping("/admin")
  public InstallAdminResponse createAdmin(@RequestBody InstallAdminRequest request) {
    ensureWizardUnlocked();
    if (!InstallStatusService.STATE_EMPTY.equals(installStatusService.getSnapshot().schemaState())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT,
          InstallStatusService.STATE_NOT_INSTALLED.equals(installStatusService.getSnapshot().schemaState())
              ? "请先完成「初始化数据库」步骤"
              : "系统已存在用户，无需创建初始管理员");
    }
    InstallAdminResponse response = installSetupService.createAdmin(request);
    installStatusService.evictCache();
    return response;
  }

  /**
   * 完成安装（需已建管理员，写入 install_state.installed_at 完成标记 + install.lock 锁文件）。
   * 注意：创建管理员后 installed 即为 true，因此本端点的「已装完」守卫用防重装锁判断，
   * 否则 finish 自身永远 403。
   */
  @PostMapping("/finish")
  public InstallFinishResponse finish() {
    if (installStatusService.isLocked()) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "系统已完成安装，安装向导已关闭");
    }
    if (!InstallStatusService.STATE_INSTALLED.equals(installStatusService.getSnapshot().schemaState())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "请先完成数据库安装与管理员创建");
    }
    InstallFinishResponse response = installSetupService.finish();
    installStatusService.evictCache();
    return response;
  }

  /** 防重装锁命中（install.lock 文件或完成标记）后写操作端点统一 403 */
  private void ensureWizardUnlocked() {
    if (installStatusService.isLocked()) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "系统已完成安装，安装向导已关闭");
    }
  }
}

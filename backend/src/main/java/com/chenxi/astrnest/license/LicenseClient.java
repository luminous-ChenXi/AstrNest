package com.chenxi.astrnest.license;

import com.chenxi.astrnest.license.dto.LicenseVerifyRequest;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

/**
 * 正版授权客户端（N2 占位的 SDK 骨架，统一规范第 3 项）。
 *
 * <p><b>行为口径</b>：</p>
 * <ul>
 *   <li>{@code enabled=false}（默认）：一切方法为空转，状态恒为 {@link LicenseState#DISABLED}；
 *       本地注册登录、上传、图集等全部功能不受任何影响；</li>
 *   <li>{@code enabled=true}：启动时与每 6 小时定时调用一次 verify-url；成功结果本地缓存
 *       {@code cache-days}（默认 7 天）内不重复请求；</li>
 *   <li>verify 不可达（网络故障/服务端未部署）：进入离线宽限 {@code offline-grace-days}
 *       （默认 3 天）内放行（{@link LicenseState#GRACE}）；</li>
 *   <li>超宽限期：状态 {@link LicenseState#OVERDUE}——<b>仅提示，绝不锁数据</b>。
 *       任何情况下本类都不会阻断业务请求、不会禁用账号、不会删除/隐藏数据；
 *       前端通过 GET /api/license/status 读取状态展示横幅，提醒站长完成授权。</li>
 * </ul>
 *
 * <p>verify 协议只到接口层：请求体 {@link LicenseVerifyRequest}、响应 {@link LicenseVerifyResult}
 * 均为最小占位结构，N2 阶段按"辰汐授权服务"最终协议扩展，扩展点集中在本类。</p>
 */
@Slf4j
@Component
public class LicenseClient {

  /** 定时校验间隔：即使缓存未过期也保持低频心跳，便于服务端侧统计在线站点（N2 口径可调）。 */
  private static final Duration VERIFY_INTERVAL = Duration.ofHours(6);
  private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(5);

  private final ChenxiLicenseProperties properties;
  private final RestClient restClient;

  /** 当前状态快照（volatile 保证跨线程可见；状态迁移全部由 verify() 单点写入）。 */
  private final AtomicReference<Snapshot> snapshot =
      new AtomicReference<>(new Snapshot(LicenseState.DISABLED, null, null));

  public LicenseClient(ChenxiLicenseProperties properties) {
    this.properties = properties;
    this.restClient = buildClient(HTTP_TIMEOUT);
  }

  /** 启动后立即做一次校验（异步定时器线程执行，不阻塞启动）。 */
  @Scheduled(initialDelay = 15_000L, fixedDelay = 6 * 60 * 60 * 1000L)
  public void scheduledVerify() {
    verify();
  }

  /**
   * 执行一次授权校验并更新状态。所有异常就地兜底为离线宽限，绝不向上抛。
   * 状态迁移：DISABLED（未启用，直接返回）→ 缓存期内跳过 → verify 成功 ACTIVE / 失败按宽限判定。
   */
  public void verify() {
    if (!properties.isEnabled()) {
      snapshot.set(new Snapshot(LicenseState.DISABLED, null, null));
      return;
    }
    Snapshot current = snapshot.get();
    // 缓存期内（cache-days）不再请求，避免每次心跳都打授权服务
    if (current.state() == LicenseState.ACTIVE && current.cachedUntil() != null
        && Instant.now().isBefore(current.cachedUntil())) {
      return;
    }
    if (!StringUtils.hasText(properties.getVerifyUrl())) {
      // N2 前占位：开关已开但未配置端点，按离线宽限处理并打一次 DEBUG，避免刷日志
      log.debug("chenxi.license.verify-url 未配置，按离线宽限处理");
      transitionOnVerifyFailure(current, "verify-url 未配置");
      return;
    }
    try {
      LicenseVerifyResult result = restClient.post()
          .uri(properties.getVerifyUrl().trim())
          .contentType(MediaType.APPLICATION_JSON)
          .body(new LicenseVerifyRequest(Instant.now()))
          .retrieve()
          .body(LicenseVerifyResult.class);
      if (result != null && result.valid()) {
        Instant cachedUntil = Instant.now().plus(Duration.ofDays(properties.getCacheDays()));
        snapshot.set(new Snapshot(LicenseState.ACTIVE, Instant.now(), cachedUntil));
        log.info("授权校验通过：licenseId={}，缓存至 {}", result.licenseId(), cachedUntil);
      } else {
        snapshot.set(new Snapshot(LicenseState.INVALID, current.lastVerifiedAt(), null));
        log.warn("授权校验未通过（站点功能不受影响，仅前端提示）");
      }
    } catch (Exception exception) {
      log.warn("授权校验请求失败（站点功能不受影响，进入离线宽限判定）：{}", exception.getMessage());
      transitionOnVerifyFailure(current, exception.getMessage());
    }
  }

  /** 当前授权状态摘要（供公开状态端点读取）。 */
  public LicenseStatusResponse currentStatus() {
    if (!properties.isEnabled()) {
      return LicenseStatusResponse.disabled();
    }
    Snapshot current = snapshot.get();
    return new LicenseStatusResponse(
        true,
        current.state(),
        current.state().needsBanner(),
        current.lastVerifiedAt(),
        null,
        describe(current.state())
    );
  }

  /** verify 不可达时的宽限判定：宽限期内 GRACE，超期 OVERDUE（仅提示）。 */
  private void transitionOnVerifyFailure(Snapshot current, String reason) {
    Instant now = Instant.now();
    if (current.lastVerifiedAt() == null) {
      // 从未成功校验过：宽限起点视为进程启动时刻，避免"永远 GRACE"
      snapshot.set(new Snapshot(LicenseState.GRACE, now, null));
      return;
    }
    Instant graceDeadline = current.lastVerifiedAt()
        .plus(Duration.ofDays(Math.max(0, properties.getOfflineGraceDays())));
    LicenseState next = now.isBefore(graceDeadline) ? LicenseState.GRACE : LicenseState.OVERDUE;
    if (next != current.state()) {
      log.warn("授权进入状态 {}（原因：{}）。站点功能不受影响，仅前端横幅提示。", next, reason);
    }
    snapshot.set(new Snapshot(next, current.lastVerifiedAt(), null));
  }

  private String describe(LicenseState state) {
    return switch (state) {
      case DISABLED -> "正版授权校验未启用";
      case ACTIVE -> "授权有效";
      case GRACE -> "授权服务暂不可达，离线宽限期内一切功能正常";
      case OVERDUE -> "离线宽限期已过：功能不受影响，请联系作者完成正版授权";
      case INVALID -> "授权无效或已过期：功能不受影响，请联系作者处理";
    };
  }

  private static RestClient buildClient(Duration timeout) {
    var requestFactory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(timeout);
    requestFactory.setReadTimeout(timeout);
    return RestClient.builder()
        .requestFactory(requestFactory)
        .build();
  }

  /** 不可变状态快照：state 与时间戳绑定，读侧原子可见。 */
  private record Snapshot(LicenseState state, Instant lastVerifiedAt, Instant cachedUntil) {
  }
}

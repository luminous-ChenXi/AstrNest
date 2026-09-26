package com.chenxi.astrnest.passport;

import com.chenxi.astrnest.common.ClientIpResolver;
import com.chenxi.astrnest.passport.dto.SsoConfigResponse;
import com.chenxi.astrnest.passport.dto.SsoExchangeRequest;
import com.chenxi.astrnest.user.dto.LoginResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * 通行证登录公开接口（{@code /api/auth/sso/**}，默认关闭，配置命名空间 {@code chenxi.passport.*}）。
 *
 * <ul>
 *   <li>{@code GET /api/auth/sso/config}：返回身份源配置与端点，关闭时 enabled=false，前端隐藏入口；</li>
 *   <li>{@code POST /api/auth/sso/exchange}：OIDC 授权码 + PKCE（code + codeVerifier，推荐）
 *       或遗留 access_token 直换，换取本地 JWT（同本地登录响应）。</li>
 * </ul>
 *
 * <p>防滥用：AuthProtectionService 的锁定维度按「用户名+IP」设计，SSO exchange 无用户名语义，
 * 硬套会与本地登录的 IP 锁互相耦合（例如配置失误把同 IP 的本地登录一并拉黑一年），
 * 因此这里采用独立内存限流：每 IP 每分钟最多 10 次 exchange 尝试（模式参考 GuestUploadService）。
 */
@Slf4j
@RestController
@RequestMapping("/api/auth/sso")
@RequiredArgsConstructor
public class SsoAuthController {

  /** 每 IP 每分钟允许的 exchange 尝试上限。 */
  private static final int EXCHANGE_LIMIT_PER_MINUTE = 10;
  private static final long WINDOW_MILLIS = 60_000L;

  private final ChenxiPassportProperties ssoProperties;
  private final SsoIdentityService ssoIdentityService;
  private final ClientIpResolver clientIpResolver;

  /** IP -> 滑动窗口计数（窗口起点 + 次数），定时惰性清理过期条目。 */
  private final Map<String, IpWindow> exchangeWindows = new ConcurrentHashMap<>();
  private volatile Instant lastCleanup = Instant.now();

  @GetMapping("/config")
  public SsoConfigResponse config() {
    if (!effectivelyEnabled()) {
      // 关闭或配置不完整：仅返回 enabled=false，前端据此隐藏 SSO 入口
      return SsoConfigResponse.disabled();
    }
    return new SsoConfigResponse(
        true,
        ssoProperties.getIssuer(),
        ssoProperties.getClientId(),
        ssoProperties.getScopes(),
        ssoProperties.getRedirectUri(),
        issuerBase() + "/oauth2/authorize",
        issuerBase() + "/oauth2/token"
    );
  }

  @PostMapping("/exchange")
  public LoginResponse exchange(@RequestBody SsoExchangeRequest request, HttpServletRequest httpRequest) {
    if (!effectivelyEnabled()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "通行证登录未启用");
    }
    if (request == null || !request.hasCredential()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "缺少登录凭证：请提供 code + codeVerifier 或 accessToken");
    }
    String ip = clientIpResolver.resolve(httpRequest);
    ensureExchangeAllowed(ip);
    if (StringUtils.hasText(request.code()) && StringUtils.hasText(request.codeVerifier())) {
      // OIDC 授权码 + PKCE：由后端完成换 token，浏览器不接触通行证 token
      return ssoIdentityService.exchangeByAuthorizationCode(request.code(), request.codeVerifier(), httpRequest);
    }
    // 遗留路径：前端自行换好 access_token，后端回站自省校验
    return ssoIdentityService.exchangeByAccessToken(request.accessToken(), httpRequest);
  }

  /** enabled 且 issuer/clientId 均已配置才视为真正可用，避免半配置状态下前端拿到残缺端点。 */
  private boolean effectivelyEnabled() {
    return ssoProperties.isEnabled()
        && StringUtils.hasText(ssoProperties.getIssuer())
        && StringUtils.hasText(ssoProperties.getClientId());
  }

  private String issuerBase() {
    String issuer = ssoProperties.getIssuer().trim();
    return issuer.endsWith("/") ? issuer.substring(0, issuer.length() - 1) : issuer;
  }

  private void ensureExchangeAllowed(String ip) {
    cleanupIfNeeded();
    IpWindow window = exchangeWindows.computeIfAbsent(ip, key -> new IpWindow());
    long now = Instant.now().toEpochMilli();
    if (now - window.startMillis >= WINDOW_MILLIS) {
      synchronized (window) {
        if (now - window.startMillis >= WINDOW_MILLIS) {
          window.startMillis = now;
          window.count.set(0);
        }
      }
    }
    if (window.count.incrementAndGet() > EXCHANGE_LIMIT_PER_MINUTE) {
      log.warn("SSO exchange 触发 IP 限流：ip={}, limit={}次/分钟", ip, EXCHANGE_LIMIT_PER_MINUTE);
      throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "SSO 登录尝试过于频繁，请稍后再试");
    }
  }

  /** 惰性清理 5 分钟内无活动的窗口，防止 map 无界增长（低频路径，开销可忽略）。 */
  private void cleanupIfNeeded() {
    Instant now = Instant.now();
    if (now.isBefore(lastCleanup.plusSeconds(300))) {
      return;
    }
    lastCleanup = now;
    long cutoff = now.minusMillis(5 * WINDOW_MILLIS).toEpochMilli();
    exchangeWindows.entrySet().removeIf(entry -> entry.getValue().startMillis < cutoff);
  }

  private static final class IpWindow {
    private volatile long startMillis = Instant.now().toEpochMilli();
    private final AtomicInteger count = new AtomicInteger();
  }
}

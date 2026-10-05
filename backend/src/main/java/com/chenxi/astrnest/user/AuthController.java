package com.chenxi.astrnest.user;

import com.chenxi.astrnest.user.dto.LoginRequest;
import com.chenxi.astrnest.user.dto.LoginResponse;
import com.chenxi.astrnest.user.dto.TwoFactorSetupConfirmResponse;
import com.chenxi.astrnest.user.dto.TwoFactorVerifyRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 本地账号认证端点：密码登录 + TOTP 二步验证挑战/绑定确认。
 * 三个端点均为匿名可访问（见 SecurityConfig permitAll），安全由防爆破服务与
 * 短期过渡令牌（purpose=2fa，5 分钟、不可作访问令牌）保证。
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  /** 登录端点进程内限流：兜底防超大密码体反复触发 BCrypt(12) 打满 CPU（账号锁定由防爆破服务承担） */
  private static final int LOGIN_LIMIT_PER_MINUTE = 30;
  private static final java.time.Duration LOGIN_RATE_WINDOW = java.time.Duration.ofMinutes(1);

  private final AuthService authService;
  private final com.chenxi.astrnest.common.ClientIpResolver clientIpResolver;
  private final com.chenxi.astrnest.common.InMemoryRateLimiter rateLimiter;

  @PostMapping("/login")
  public LoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
    String ip = clientIpResolver.resolve(httpRequest);
    if (rateLimiter.tryAcquire("login:" + ip, LOGIN_LIMIT_PER_MINUTE, LOGIN_RATE_WINDOW)) {
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.TOO_MANY_REQUESTS, "请求过于频繁，请稍后再试");
    }
    return authService.login(request, httpRequest);
  }

  /** 二步验证挑战：6 位动态码或 8 位一次性还原码 → 换发正式 JWT。 */
  @PostMapping("/2fa/verify")
  public LoginResponse verifyTwoFactor(@Valid @RequestBody TwoFactorVerifyRequest request,
      HttpServletRequest httpRequest) {
    return authService.verifyTwoFactor(request, httpRequest);
  }

  /** 强制绑定确认：6 位动态码 → 绑定落库 + 发放一次性还原码 + 直接完成登录。 */
  @PostMapping("/2fa/setup/confirm")
  public TwoFactorSetupConfirmResponse confirmTwoFactorSetup(@Valid @RequestBody TwoFactorVerifyRequest request) {
    return authService.confirmTwoFactorSetup(request);
  }
}

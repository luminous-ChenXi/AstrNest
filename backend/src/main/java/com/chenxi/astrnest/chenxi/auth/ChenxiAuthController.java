package com.chenxi.astrnest.chenxi.auth;

import com.chenxi.astrnest.chenxi.captcha.ChenxiCaptchaService;
import com.chenxi.astrnest.chenxi.captcha.dto.ChenxiCaptchaChallengeResponse;
import com.chenxi.astrnest.chenxi.captcha.dto.ChenxiCaptchaVerifyRequest;
import com.chenxi.astrnest.chenxi.captcha.dto.ChenxiCaptchaVerifyResponse;
import com.chenxi.astrnest.chenxi.auth.dto.RegisterAccountRequest;
import com.chenxi.astrnest.chenxi.auth.dto.RequestEmailCodeRequest;
import com.chenxi.astrnest.chenxi.auth.dto.ResetPasswordRequest;
import com.chenxi.astrnest.chenxi.auth.dto.VerifyEmailCodeRequest;
import com.chenxi.astrnest.common.ClientIpResolver;
import com.chenxi.astrnest.common.InMemoryRateLimiter;
import com.chenxi.astrnest.security.bruteforce.AuthProtectionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Duration;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth/chenxi")
@RequiredArgsConstructor
public class ChenxiAuthController {

  /** 匿名热点端点兜底限流：验证码图片每 IP 每分钟 20 次、邮箱占用检查每 IP 每分钟 10 次 */
  private static final int CAPTCHA_LIMIT_PER_MINUTE = 20;
  private static final int CHECK_EMAIL_LIMIT_PER_MINUTE = 10;
  private static final Duration RATE_WINDOW = Duration.ofMinutes(1);

  private final ChenxiCaptchaService captchaService;
  private final ChenxiAuthService authService;
  private final AuthProtectionService authProtectionService;
  private final ClientIpResolver clientIpResolver;
  private final InMemoryRateLimiter rateLimiter;

  @PostMapping("/captcha")
  public ChenxiCaptchaChallengeResponse createCaptcha(HttpServletRequest httpRequest) {
    String ip = clientIpResolver.resolve(httpRequest);
    // CAPTCHA_IP 锁（10 次失败锁 24h）在此评估；随后进程内限流防验证码挑战被灌爆（每调用写一行票表）
    authProtectionService.ensureCaptchaAllowed(ip);
    if (rateLimiter.tryAcquire("captcha:" + ip, CAPTCHA_LIMIT_PER_MINUTE, RATE_WINDOW)) {
      throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "请求过于频繁，请稍后再试");
    }
    return captchaService.createChallenge();
  }

  @PostMapping("/captcha/verify")
  public ChenxiCaptchaVerifyResponse verifyCaptcha(
      @Valid @RequestBody ChenxiCaptchaVerifyRequest request,
      HttpServletRequest httpRequest) {
    String ip = clientIpResolver.resolve(httpRequest);
    authProtectionService.ensureCaptchaAllowed(ip);
    authProtectionService.ensureRegisterAllowed("", ip);
    ChenxiCaptchaVerifyResponse response = captchaService.verifyChallenge(request);
    if (response.passed()) {
      authProtectionService.clearCaptchaFailures(ip);
    } else {
      authProtectionService.recordCaptchaFailure(ip);
    }
    return response;
  }

  @PostMapping("/register/code")
  public Map<String, String> requestRegisterCode(@Valid @RequestBody RequestEmailCodeRequest request, HttpServletRequest httpRequest) {
    String ip = clientIpResolver.resolve(httpRequest);
    authProtectionService.ensureRegisterAllowed(request.email(), ip);
    authService.requestRegisterCode(request.email(), request.captchaToken());
    return Map.of("message", "若该邮箱可用，验证码已发送，请查收");
  }

  @PostMapping("/register/verify-code")
  public Map<String, String> verifyRegisterCode(@Valid @RequestBody VerifyEmailCodeRequest request, HttpServletRequest httpRequest) {
    String ip = clientIpResolver.resolve(httpRequest);
    authProtectionService.ensureRegisterAllowed(request.email(), ip);
    authService.verifyEmailCode(request.email(), request.code(), ChenxiEmailScene.REGISTER);
    return Map.of("message", "验证码正确");
  }

  @PostMapping("/register")
  public Map<String, String> register(@Valid @RequestBody RegisterAccountRequest request, HttpServletRequest httpRequest) {
    String ip = clientIpResolver.resolve(httpRequest);
    authProtectionService.ensureRegisterAllowed(request.username(), ip);
    try {
      authService.registerUser(request.email(), request.code(), request.linkToken(),
          request.username(), request.displayName(), request.password());
    } catch (ResponseStatusException | IllegalArgumentException exception) {
      // 注册失败计数此前从未接入（recordRegisterFailure 死代码），批量探测零成本
      authProtectionService.recordRegisterFailure(request.username(), ip);
      throw exception;
    }
    // 注册成功只清「账号+IP」维度：不能像登录那样清 IP_ONLY，否则被锁 IP 可借注册自助解锁
    authProtectionService.recordRegistrationSuccess(request.username(), ip);
    return Map.of("message", "注册成功，快去登录吧");
  }

  @PostMapping("/password/code")
  public Map<String, String> requestPasswordCode(@Valid @RequestBody RequestEmailCodeRequest request, HttpServletRequest httpRequest) {
    String ip = clientIpResolver.resolve(httpRequest);
    authProtectionService.ensureRegisterAllowed(request.email(), ip);
    authService.requestResetCode(request.email(), request.captchaToken());
    return Map.of("message", "若该邮箱已注册，验证码已发送，请查收");
  }

  @PostMapping("/password/reset")
  public Map<String, String> resetPassword(@Valid @RequestBody ResetPasswordRequest request, HttpServletRequest httpRequest) {
    String ip = clientIpResolver.resolve(httpRequest);
    authProtectionService.ensureRegisterAllowed(request.email(), ip);
    authService.resetPassword(request.email(), request.code(), request.newPassword());
    // 同注册：只清账号维度，保留 IP 维度爆破锁定
    authProtectionService.recordRegistrationSuccess(request.email(), ip);
    return Map.of("message", "密码已重置，可使用新密码登录");
  }

  @GetMapping("/check-email")
  public Map<String, Boolean> checkEmailAvailability(@RequestParam String email, HttpServletRequest httpRequest) {
    String ip = clientIpResolver.resolve(httpRequest);
    // 匿名布尔接口可批量枚举注册邮箱：进程内限流兜底（10 次/分/IP）
    if (rateLimiter.tryAcquire("check-email:" + ip, CHECK_EMAIL_LIMIT_PER_MINUTE, RATE_WINDOW)) {
      throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "请求过于频繁，请稍后再试");
    }
    boolean available = authService.isEmailAvailable(email);
    return Map.of("available", available);
  }
}

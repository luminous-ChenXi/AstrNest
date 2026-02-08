package com.chenxi.astrnest.chenxi.auth;

import com.chenxi.astrnest.chenxi.captcha.ChenxiCaptchaService;
import com.chenxi.astrnest.chenxi.captcha.dto.ChenxiCaptchaChallengeResponse;
import com.chenxi.astrnest.chenxi.captcha.dto.ChenxiCaptchaVerifyRequest;
import com.chenxi.astrnest.chenxi.captcha.dto.ChenxiCaptchaVerifyResponse;
import com.chenxi.astrnest.chenxi.auth.dto.RegisterAccountRequest;
import com.chenxi.astrnest.chenxi.auth.dto.RequestEmailCodeRequest;
import com.chenxi.astrnest.chenxi.auth.dto.ResetPasswordRequest;
import com.chenxi.astrnest.security.bruteforce.AuthProtectionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/chenxi")
@RequiredArgsConstructor
public class ChenxiAuthController {

  private final ChenxiCaptchaService captchaService;
  private final ChenxiAuthService authService;
  private final AuthProtectionService authProtectionService;

  @PostMapping("/captcha")
  public ChenxiCaptchaChallengeResponse createCaptcha() {
    return captchaService.createChallenge();
  }

  @PostMapping("/captcha/verify")
  public ChenxiCaptchaVerifyResponse verifyCaptcha(
      @Valid @RequestBody ChenxiCaptchaVerifyRequest request,
      HttpServletRequest httpRequest) {
    String ip = resolveClientIp(httpRequest);
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
    String ip = resolveClientIp(httpRequest);
    authProtectionService.ensureRegisterAllowed(request.email(), ip);
    authService.requestRegisterCode(request.email(), request.captchaToken());
    return Map.of("message", "验证码已发送至邮箱");
  }

  @PostMapping("/register")
  public Map<String, String> register(@Valid @RequestBody RegisterAccountRequest request, HttpServletRequest httpRequest) {
    String ip = resolveClientIp(httpRequest);
    authProtectionService.ensureRegisterAllowed(request.username(), ip);
    authService.registerUser(request.email(), request.code(), request.username(), request.displayName(), request.password());
    authProtectionService.recordLoginSuccess(request.username(), ip);
    return Map.of("message", "注册成功，快去登录吧");
  }

  @PostMapping("/password/code")
  public Map<String, String> requestPasswordCode(@Valid @RequestBody RequestEmailCodeRequest request, HttpServletRequest httpRequest) {
    String ip = resolveClientIp(httpRequest);
    authProtectionService.ensureRegisterAllowed(request.email(), ip);
    authService.requestResetCode(request.email(), request.captchaToken());
    return Map.of("message", "验证码已发送，请查收邮箱");
  }

  @PostMapping("/password/reset")
  public Map<String, String> resetPassword(@Valid @RequestBody ResetPasswordRequest request, HttpServletRequest httpRequest) {
    String ip = resolveClientIp(httpRequest);
    authProtectionService.ensureRegisterAllowed(request.email(), ip);
    authService.resetPassword(request.email(), request.code(), request.newPassword());
    authProtectionService.recordLoginSuccess(request.email(), ip);
    return Map.of("message", "密码已重置，可使用新密码登录");
  }

  private String resolveClientIp(HttpServletRequest request) {
    if (request == null) return "unknown";
    String[] headers = {"X-Forwarded-For", "X-Real-IP", "CF-Connecting-IP"};
    for (String header : headers) {
      String value = request.getHeader(header);
      if (value != null && !value.isBlank()) {
        return value.split(",")[0].trim();
      }
    }
    return request.getRemoteAddr();
  }
}

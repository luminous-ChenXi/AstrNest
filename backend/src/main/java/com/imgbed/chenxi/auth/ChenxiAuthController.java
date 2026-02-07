package com.imgbed.chenxi.auth;

import com.imgbed.chenxi.captcha.ChenxiCaptchaService;
import com.imgbed.chenxi.captcha.dto.ChenxiCaptchaChallengeResponse;
import com.imgbed.chenxi.captcha.dto.ChenxiCaptchaVerifyRequest;
import com.imgbed.chenxi.captcha.dto.ChenxiCaptchaVerifyResponse;
import com.imgbed.chenxi.auth.dto.RegisterAccountRequest;
import com.imgbed.chenxi.auth.dto.RequestEmailCodeRequest;
import com.imgbed.chenxi.auth.dto.ResetPasswordRequest;
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

  @PostMapping("/captcha")
  public ChenxiCaptchaChallengeResponse createCaptcha() {
    return captchaService.createChallenge();
  }

  @PostMapping("/captcha/verify")
  public ChenxiCaptchaVerifyResponse verifyCaptcha(@Valid @RequestBody ChenxiCaptchaVerifyRequest request) {
    return captchaService.verifyChallenge(request);
  }

  @PostMapping("/register/code")
  public Map<String, String> requestRegisterCode(@Valid @RequestBody RequestEmailCodeRequest request) {
    authService.requestRegisterCode(request.email(), request.captchaToken());
    return Map.of("message", "验证码已发送至邮箱");
  }

  @PostMapping("/register")
  public Map<String, String> register(@Valid @RequestBody RegisterAccountRequest request) {
    authService.registerUser(request.email(), request.code(), request.username(), request.displayName(), request.password());
    return Map.of("message", "注册成功，快去登录吧");
  }

  @PostMapping("/password/code")
  public Map<String, String> requestPasswordCode(@Valid @RequestBody RequestEmailCodeRequest request) {
    authService.requestResetCode(request.email(), request.captchaToken());
    return Map.of("message", "验证码已发送，请查收邮箱");
  }

  @PostMapping("/password/reset")
  public Map<String, String> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
    authService.resetPassword(request.email(), request.code(), request.newPassword());
    return Map.of("message", "密码已重置，可使用新密码登录");
  }
}

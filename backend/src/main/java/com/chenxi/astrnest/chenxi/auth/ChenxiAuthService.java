package com.chenxi.astrnest.chenxi.auth;

import com.chenxi.astrnest.chenxi.captcha.ChenxiCaptchaService;
import com.chenxi.astrnest.chenxi.mail.ChenxiMailService;
import com.chenxi.astrnest.security.user.UserAccount;
import com.chenxi.astrnest.security.user.UserAccountRepository;
import com.chenxi.astrnest.security.user.UserRole;
import com.chenxi.astrnest.security.user.UserRoleRepository;
import com.chenxi.astrnest.system.SystemConfigService;
import jakarta.transaction.Transactional;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ChenxiAuthService {

  private static final SecureRandom RANDOM = new SecureRandom();

  private final ChenxiCaptchaService captchaService;
  private final ChenxiMailService mailService;
  private final ChenxiEmailTokenRepository emailTokenRepository;
  private final UserAccountRepository userAccountRepository;
  private final UserRoleRepository userRoleRepository;
  private final PasswordEncoder passwordEncoder;
  private final SystemConfigService systemConfigService;

  @Transactional
  public void requestRegisterCode(String email, String captchaToken) {
    ensureRegistrationEnabled();
    captchaService.consumeCertificationOrThrow(captchaToken);
    String normalizedEmail = normalizeEmail(email);
    if (userAccountRepository.existsByEmail(normalizedEmail)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "该邮箱已绑定账号，可直接登录");
    }
    sendEmailCode(normalizedEmail, ChenxiEmailScene.REGISTER, captchaToken);
  }

  @Transactional
  public void requestResetCode(String email, String captchaToken) {
    captchaService.consumeCertificationOrThrow(captchaToken);
    String normalizedEmail = normalizeEmail(email);
    if (!userAccountRepository.existsByEmail(normalizedEmail)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "未找到该邮箱对应的账号");
    }
    sendEmailCode(normalizedEmail, ChenxiEmailScene.PASSWORD_RESET, captchaToken);
  }

  @Transactional
  public void registerUser(String email, String code, String username, String displayName, String password) {
    ensureRegistrationEnabled();
    String normalizedEmail = normalizeEmail(email);
    ChenxiEmailToken token = consumeVerificationCode(normalizedEmail, ChenxiEmailScene.REGISTER, code);
    if (userAccountRepository.existsByUsername(username)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "用户名已存在");
    }
    if (userAccountRepository.existsByEmail(normalizedEmail)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "该邮箱已注册");
    }
    UserAccount user = new UserAccount();
    user.setUsername(username);
    user.setPassword(passwordEncoder.encode(password));
    user.setDisplayName(StringUtils.hasText(displayName) ? displayName : username);
    user.setEmail(normalizedEmail);
    user.setActive(true);
    user.setDailyUploadLimit(100);
    user.setStorageQuotaMb(200L);
    UserRole userRole = userRoleRepository.findByName("USER")
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "缺少 USER 角色"));
    user.getRoles().add(userRole);
    userAccountRepository.save(user);
    token.setConsumed(true);
    token.setConsumedAt(Instant.now());
    emailTokenRepository.save(token);
  }

  @Transactional
  public void resetPassword(String email, String code, String newPassword) {
    String normalizedEmail = normalizeEmail(email);
    ChenxiEmailToken token = consumeVerificationCode(normalizedEmail, ChenxiEmailScene.PASSWORD_RESET, code);
    UserAccount user = userAccountRepository.findByEmail(normalizedEmail)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "账号不存在"));
    user.setPassword(passwordEncoder.encode(newPassword));
    userAccountRepository.save(user);
    token.setConsumed(true);
    token.setConsumedAt(Instant.now());
    emailTokenRepository.save(token);
  }

  private void sendEmailCode(String email, ChenxiEmailScene scene, String captchaToken) {
    Instant now = Instant.now();
    emailTokenRepository.findTopByEmailAndSceneOrderByCreatedAtDesc(email, scene).ifPresent(latest -> {
      if (!latest.isConsumed() && latest.getResendAvailableAt().isAfter(now)) {
        throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "验证码发送过于频繁，请稍后再试");
      }
    });
    long hourly = emailTokenRepository.countByEmailAndSceneAndCreatedAtAfter(email, scene, now.minus(1, ChronoUnit.HOURS));
    if (hourly >= 6) {
      throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "请求次数过多，请稍后再试");
    }
    ChenxiEmailToken token = new ChenxiEmailToken();
    token.setEmail(email);
    token.setScene(scene);
    token.setCode(generateCode());
    token.setExpiresAt(now.plus(5, ChronoUnit.MINUTES));
    token.setResendAvailableAt(now.plus(60, ChronoUnit.SECONDS));
    token.setCaptchaToken(captchaToken);
    emailTokenRepository.save(token);
    mailService.sendVerificationMail(email, token.getCode(), scene);
  }

  private void ensureRegistrationEnabled() {
    if (!systemConfigService.isRegistrationEnabled()) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "当前已关闭开放注册，请联系管理员");
    }
  }

  private ChenxiEmailToken consumeVerificationCode(String email, ChenxiEmailScene scene, String code) {
    ChenxiEmailToken token = emailTokenRepository.findTopByEmailAndSceneAndConsumedFalseOrderByCreatedAtDesc(email, scene)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "请先获取验证码"));
    Instant now = Instant.now();
    if (token.getExpiresAt().isBefore(now)) {
      token.setConsumed(true);
      emailTokenRepository.save(token);
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "验证码已失效，请重新获取");
    }
    if (!token.getCode().equalsIgnoreCase(code)) {
      token.setAttempts(token.getAttempts() + 1);
      if (token.getAttempts() >= 5) {
        token.setConsumed(true);
      }
      emailTokenRepository.save(token);
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "验证码不正确");
    }
    return token;
  }

  private String generateCode() {
    int value = RANDOM.nextInt(900_000) + 100_000;
    return Integer.toString(value);
  }

  private String normalizeEmail(String email) {
    if (!StringUtils.hasText(email)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "邮箱不能为空");
    }
    return email.trim().toLowerCase(Locale.ROOT);
  }
}

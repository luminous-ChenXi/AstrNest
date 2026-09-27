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
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChenxiAuthService {

  private static final SecureRandom RANDOM = new SecureRandom();
  /** 注册场景验证码/链接有效期：30 分钟（找回密码保持 5 分钟） */
  private static final long REGISTER_TOKEN_MINUTES = 30L;
  private static final long RESET_TOKEN_MINUTES = 5L;

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

  /**
   * 注册落库。邮箱验证行为由站点开关 registration.email_verify_required 决定：
   * <ul>
   *   <li><b>开启</b>：必须携带 6 位验证码或邮件链接令牌（linkToken）之一，校验通过后
   *       {@code email_verified=true}、账号即激活；</li>
   *   <li><b>关闭</b>（默认）：验证码/链接可选，邮箱可空，注册即激活（email_verified 按实际验证情况）。</li>
   * </ul>
   */
  @Transactional
  public void registerUser(String email, String code, String linkToken, String username,
      String displayName, String password) {
    ensureRegistrationEnabled();
    boolean verifyRequired = systemConfigService.isEmailVerifyRequired();

    String normalizedEmail = null;
    boolean verified = false;
    if (verifyRequired) {
      if (StringUtils.hasText(linkToken)) {
        normalizedEmail = consumeRegisterLinkToken(linkToken, email);
        verified = true;
      } else {
        normalizedEmail = normalizeEmail(email);
        consumeVerificationCode(normalizedEmail, ChenxiEmailScene.REGISTER, code);
        verified = true;
      }
    } else if (StringUtils.hasText(email)) {
      normalizedEmail = normalizeEmail(email);
    }

    if (userAccountRepository.existsByUsername(username)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "用户名已存在");
    }
    if (normalizedEmail != null && userAccountRepository.existsByEmail(normalizedEmail)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "该邮箱已注册");
    }
    long existingUsers = userAccountRepository.count();

    UserAccount user = new UserAccount();
    user.setUsername(username);
    user.setPassword(passwordEncoder.encode(password));
    user.setDisplayName(StringUtils.hasText(displayName) ? displayName : username);
    user.setEmail(normalizedEmail);
    user.setEmailVerified(verified);
    user.setActive(true);

    UserRole roleToAssign;
    if (existingUsers == 0) {
      // 首个注册用户自动成为管理员
      roleToAssign = userRoleRepository.findByName("ADMIN")
          .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "缺少 ADMIN 角色"));
      user.setDailyUploadLimit(null);
      user.setStorageQuotaMb(null);
    } else {
      // 后续注册用户默认为普通用户角色，拥有上传权限
      roleToAssign = userRoleRepository.findByName("USER")
          .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "缺少 USER 角色"));
      user.setDailyUploadLimit(50);
      user.setStorageQuotaMb(200L);
    }
    user.getRoles().add(roleToAssign);

    userAccountRepository.save(user);
    log.info("Local register: user={} emailVerified={} verifyRequired={}", username, verified, verifyRequired);
  }

  @Transactional
  public void resetPassword(String email, String code, String newPassword) {
    String normalizedEmail = normalizeEmail(email);
    consumeVerificationCode(normalizedEmail, ChenxiEmailScene.PASSWORD_RESET, code);
    UserAccount user = userAccountRepository.findByEmail(normalizedEmail)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "账号不存在"));
    user.setPassword(passwordEncoder.encode(newPassword));
    userAccountRepository.save(user);
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
    long ttlMinutes = scene == ChenxiEmailScene.REGISTER ? REGISTER_TOKEN_MINUTES : RESET_TOKEN_MINUTES;
    token.setExpiresAt(now.plus(ttlMinutes, ChronoUnit.MINUTES));
    token.setResendAvailableAt(now.plus(60, ChronoUnit.SECONDS));
    token.setCaptchaToken(captchaToken);
    if (scene == ChenxiEmailScene.REGISTER) {
      // 注册场景附带链接令牌：邮件中同时给出 6 位验证码与「点链接完成验证」入口
      token.setLinkToken(generateLinkToken());
    }
    emailTokenRepository.save(token);
    mailService.sendVerificationMail(email, token.getCode(), scene,
        scene == ChenxiEmailScene.REGISTER ? token.getLinkToken() : null);
  }

  private void ensureRegistrationEnabled() {
    if (!systemConfigService.isRegistrationEnabled()) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "当前已关闭开放注册，请联系管理员");
    }
  }

  /**
   * 验证邮箱验证码是否正确（校验通过但不消耗验证码）。
   */
  public void verifyEmailCode(String email, String code, ChenxiEmailScene scene) {
    verifyAndConsume(email, scene, code, false);
  }

  /**
   * 验证邮箱验证码并在校验通过后消耗验证码（同一事务内，后续业务失败会随事务回滚）。
   */
  private ChenxiEmailToken consumeVerificationCode(String email, ChenxiEmailScene scene, String code) {
    return verifyAndConsume(email, scene, code, true);
  }

  /**
   * 校验并消费注册链接令牌：令牌存在、未消费、未过期且与邮箱匹配时消耗之，返回规范化邮箱。
   * 「点链接」与「输码」殊途同归——都收敛到同一条验证码记录上。
   */
  private String consumeRegisterLinkToken(String linkToken, String rawEmail) {
    ChenxiEmailToken token = emailTokenRepository
        .findTopByLinkTokenAndConsumedFalseOrderByCreatedAtDesc(linkToken.trim())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "注册链接无效或已被使用，请改用邮箱验证码"));
    Instant now = Instant.now();
    if (token.getExpiresAt().isBefore(now)) {
      token.setConsumed(true);
      emailTokenRepository.save(token);
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "注册链接已过期（30 分钟有效），请重新获取验证码");
    }
    if (StringUtils.hasText(rawEmail) && !token.getEmail().equalsIgnoreCase(rawEmail.trim())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "注册链接与当前邮箱不匹配");
    }
    token.setConsumed(true);
    token.setConsumedAt(now);
    emailTokenRepository.save(token);
    return token.getEmail();
  }

  /**
   * 验证码校验的唯一实现：过期/错误次数/防爆破逻辑只保留这一份。
   */
  private ChenxiEmailToken verifyAndConsume(String email, ChenxiEmailScene scene, String code, boolean consume) {
    String normalizedEmail = normalizeEmail(email);
    ChenxiEmailToken token = emailTokenRepository.findTopByEmailAndSceneAndConsumedFalseOrderByCreatedAtDesc(normalizedEmail, scene)
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
    if (consume) {
      token.setConsumed(true);
      token.setConsumedAt(Instant.now());
      emailTokenRepository.save(token);
    }
    return token;
  }

  private String generateCode() {
    int value = RANDOM.nextInt(900_000) + 100_000;
    return Integer.toString(value);
  }

  /** 32 位 URL 安全随机令牌（注册邮件链接用）。 */
  private String generateLinkToken() {
    StringBuilder builder = new StringBuilder(32);
    String alphabet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    for (int i = 0; i < 32; i++) {
      builder.append(alphabet.charAt(RANDOM.nextInt(alphabet.length())));
    }
    return builder.toString();
  }

  private String normalizeEmail(String email) {
    if (!StringUtils.hasText(email)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "邮箱不能为空");
    }
    return email.trim().toLowerCase(Locale.ROOT);
  }

  public boolean isEmailAvailable(String email) {
    String normalizedEmail = normalizeEmail(email);
    return !userAccountRepository.existsByEmail(normalizedEmail);
  }
}

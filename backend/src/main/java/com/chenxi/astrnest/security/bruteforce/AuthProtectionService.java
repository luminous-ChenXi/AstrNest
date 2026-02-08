package com.chenxi.astrnest.security.bruteforce;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthProtectionService {

  private static final int USER_SHORT_THRESHOLD = 5;
  private static final int USER_LONG_THRESHOLD = 20;
  private static final int IP_SHORT_THRESHOLD = 8;
  private static final int IP_LONG_THRESHOLD = 20;
  private static final int CAPTCHA_THRESHOLD = 10;

  private static final Duration SHORT_LOCK = Duration.ofMinutes(15);
  private static final Duration LONG_LOCK = Duration.ofHours(24);
  private static final Duration BLOCK_LOCK = Duration.ofDays(365);

  private final AuthLockStateRepository lockStateRepository;
  private final SecurityLogEntryRepository logRepository;

  private static String today() {
    return DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.CHINA)
        .withZone(ZoneId.systemDefault())
        .format(Instant.now());
  }

  public void ensureLoginAllowed(String username, String ip) {
    GuardOutcome userIp = evaluateLock(normalize(username), normalizeIp(ip), LockDimension.USER_IP);
    GuardOutcome ipOnly = evaluateLock("", normalizeIp(ip), LockDimension.IP_ONLY);
    GuardOutcome blocked = userIp.blocked ? userIp : ipOnly;
    if (blocked != null && blocked.blocked) {
      throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, blocked.message);
    }
  }

  public void recordLoginFailure(String username, String ip) {
    applyFailure(normalize(username), normalizeIp(ip), LockDimension.USER_IP, USER_SHORT_THRESHOLD, USER_LONG_THRESHOLD, "LOGIN_FAIL");
    applyFailure("", normalizeIp(ip), LockDimension.IP_ONLY, IP_SHORT_THRESHOLD, IP_LONG_THRESHOLD, "LOGIN_FAIL");
  }

  public void recordLoginSuccess(String username, String ip) {
    clearState(normalize(username), normalizeIp(ip), LockDimension.USER_IP);
    clearState("", normalizeIp(ip), LockDimension.IP_ONLY);
  }

  public void ensureRegisterAllowed(String username, String ip) {
    GuardOutcome userIp = evaluateLock(normalize(username), normalizeIp(ip), LockDimension.USER_IP);
    GuardOutcome ipOnly = evaluateLock("", normalizeIp(ip), LockDimension.IP_ONLY);
    GuardOutcome blocked = userIp.blocked ? userIp : ipOnly;
    if (blocked != null && blocked.blocked) {
      throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, blocked.message);
    }
  }

  public void recordRegisterFailure(String username, String ip) {
    applyFailure(normalize(username), normalizeIp(ip), LockDimension.USER_IP, USER_SHORT_THRESHOLD, USER_LONG_THRESHOLD, "REGISTER_FAIL");
    applyFailure("", normalizeIp(ip), LockDimension.IP_ONLY, IP_SHORT_THRESHOLD, IP_LONG_THRESHOLD, "REGISTER_FAIL");
  }

  public void recordCaptchaFailure(String ip) {
    applyCaptchaFailure(normalizeIp(ip));
  }

  public void clearCaptchaFailures(String ip) {
    clearState("", normalizeIp(ip), LockDimension.CAPTCHA_IP);
  }

  private void applyCaptchaFailure(String ip) {
    AuthLockState state = getOrCreate("", ip, LockDimension.CAPTCHA_IP);
    resetWindowIfNeeded(state);
    state.setFailCount(state.getFailCount() + 1);
    state.setLastFailedAt(Instant.now());
    log("CAPTCHA_FAIL", null, ip, "验证码校验失败");
    if (state.getFailCount() >= CAPTCHA_THRESHOLD) {
      state.setLockedUntil(Instant.now().plus(LONG_LOCK));
      state.setLockReason("验证码连续失败，注册已暂时锁定");
      state.setFailCount(0);
      log("CAPTCHA_LOCK", null, ip, state.getLockReason());
    }
    lockStateRepository.save(state);
  }

  private GuardOutcome evaluateLock(String username, String ip, LockDimension dimension) {
    return lockStateRepository.findByUsernameAndIpAndDimension(username, ip, dimension)
        .map(state -> {
          if (state.getLockedUntil() != null && state.getLockedUntil().isAfter(Instant.now())) {
            long seconds = Duration.between(Instant.now(), state.getLockedUntil()).toSeconds();
            String msg = state.getLockReason() != null
                ? state.getLockReason() + "，请在 " + seconds + " 秒后重试"
                : "当前环境已被暂时保护，请稍后再试";
            return new GuardOutcome(true, msg);
          }
          return new GuardOutcome(false, null);
        })
        .orElse(new GuardOutcome(false, null));
  }

  private void applyFailure(String username, String ip, LockDimension dimension, int shortThreshold, int longThreshold, String eventType) {
    AuthLockState state = getOrCreate(username, ip, dimension);
    resetWindowIfNeeded(state);
    state.setFailCount(state.getFailCount() + 1);
    state.setLastFailedAt(Instant.now());
    log(eventType, username, ip, "凭证校验失败");

    boolean locked = false;
    if (state.getFailCount() >= longThreshold) {
      state.setLockedUntil(Instant.now().plus(LONG_LOCK));
      state.setLockReason("累计失败过多，已锁定24小时");
      state.setLockCount(state.getLockCount() + 1);
      state.setFailCount(0);
      state.setStage(LockStage.TIGHT);
      locked = true;
    } else if (state.getFailCount() >= shortThreshold) {
      state.setLockedUntil(Instant.now().plus(SHORT_LOCK));
      state.setLockReason("连续失败过多，已锁定15分钟");
      state.setLockCount(state.getLockCount() + 1);
      state.setFailCount(0);
      locked = true;
    }

    if (state.getLockCount() >= 3 && state.getStage() != LockStage.BLOCKED) {
      state.setLockedUntil(Instant.now().plus(BLOCK_LOCK));
      state.setLockReason("多次触发保护，已暂时拉黑该环境");
      state.setStage(LockStage.BLOCKED);
      locked = true;
    }

    if (locked) {
      log(eventType.equals("REGISTER_FAIL") ? "REGISTER_LOCK" : "LOGIN_LOCK", username, ip, state.getLockReason());
    }
    lockStateRepository.save(state);
  }

  private void clearState(String username, String ip, LockDimension dimension) {
    lockStateRepository.findByUsernameAndIpAndDimension(username, ip, dimension).ifPresent(state -> {
      state.setFailCount(0);
      state.setLockCount(0);
      state.setLockedUntil(null);
      state.setLockReason(null);
      state.setStage(LockStage.INITIAL);
      lockStateRepository.save(state);
    });
  }

  private AuthLockState getOrCreate(String username, String ip, LockDimension dimension) {
    return lockStateRepository.findByUsernameAndIpAndDimension(username, ip, dimension)
        .orElseGet(() -> {
          AuthLockState s = new AuthLockState();
          s.setUsername(username);
          s.setIp(ip);
          s.setDimension(dimension);
          s.setStage(LockStage.INITIAL);
          s.setFailCount(0);
          s.setWindowDate(today());
          return s;
        });
  }

  private void resetWindowIfNeeded(AuthLockState state) {
    if (state.getWindowDate() == null || !state.getWindowDate().equals(today())) {
      state.setWindowDate(today());
      state.setStage(LockStage.INITIAL);
      state.setFailCount(0);
      state.setLockedUntil(null);
      state.setLockReason(null);
    }
    if (state.getLockedUntil() != null && state.getLockedUntil().isBefore(Instant.now())) {
      state.setLockedUntil(null);
      state.setLockReason(null);
    }
  }

  private void log(String eventType, String username, String ip, String message) {
    SecurityLogEntry entry = new SecurityLogEntry();
    entry.setEventType(eventType);
    entry.setUsername(emptyToNull(username));
    entry.setIp(emptyToNull(ip));
    entry.setMessage(message);
    logRepository.save(entry);
  }

  private String normalize(String value) {
    return value == null ? "" : value.trim();
  }

  private String normalizeIp(String ip) {
    return ip == null ? "" : ip.trim();
  }

  private String emptyToNull(String value) {
    return value == null || value.isBlank() ? null : value;
  }

  private record GuardOutcome(boolean blocked, String message) {}
}

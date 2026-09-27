package com.chenxi.astrnest.security.totp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/**
 * 用户 TOTP 账号管理：登录期强制绑定 → 确认 → 还原码一次性发放 → 日常验证 → 管理员重置。
 *
 * <p>验证口径：6 位纯数字按 TOTP（RFC 6238，±1 步窗口）校验；8 位还原码按
 * BCrypt 哈希匹配（一次性，命中即焚）。两者共用同一个挑战入口。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TotpAccountService {

  /** 还原码数量与长度（8 位，去除易混淆字符的字母数字表） */
  private static final int RECOVERY_CODE_COUNT = 10;
  private static final int RECOVERY_CODE_LENGTH = 8;
  private static final String RECOVERY_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
  /** TOTP 防重放最小计数器步进 */
  private static final SecureRandom RANDOM = new SecureRandom();
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final TotpService totpService;
  private final UserTotpRepository userTotpRepository;
  private final PasswordEncoder passwordEncoder;

  /** 用户是否已绑定并确认 TOTP。 */
  public boolean isBound(Long userId) {
    return userTotpRepository.existsByUserIdAndConfirmedTrue(userId);
  }

  /** 批量查询已绑定用户（管理端列表展示用）。 */
  public List<Long> filterBoundUserIds(List<Long> userIds) {
    if (userIds == null || userIds.isEmpty()) {
      return List.of();
    }
    return userTotpRepository.findConfirmedUserIds(userIds);
  }

  /**
   * 为未绑定用户生成（或复用）待确认的绑定信息（密钥 + otpauth URI）。
   * 密钥以「未确认绑定」落库：用户放弃绑定即成无害残留（下次登录复用同一密钥，
   * 二维码在多次尝试间保持稳定）；未确认的绑定永远无法换发 JWT。
   */
  @Transactional
  public TotpSetup startSetup(Long userId, String username) {
    UserTotp pending = userTotpRepository.findById(userId).orElse(null);
    String secret;
    if (pending != null && !pending.isConfirmed()) {
      secret = pending.getSecret();
    } else {
      secret = totpService.generateSecret();
      UserTotp binding = new UserTotp();
      binding.setUserId(userId);
      binding.setSecret(secret);
      binding.setConfirmed(false);
      binding.setRecoveryHashes("[]");
      binding = userTotpRepository.save(binding);
      pending = binding;
    }
    log.info("TOTP setup issued for user {}", userId);
    return new TotpSetup(secret, totpService.buildOtpauthUri(secret, username), pending.getCreatedAt() == null);
  }

  /**
   * 确认绑定：校验动态码 → 置为 confirmed → 生成 10 个一次性还原码明文返回。
   * 还原码明文仅此一次展示，库中只保留 BCrypt 哈希。
   */
  @Transactional
  public List<String> confirmSetup(Long userId, String code) {
    UserTotp pending = userTotpRepository.findById(userId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
            "未找到待确认的二步验证绑定，请重新登录"));
    long counter = totpService.verify(pending.getSecret(), code, Instant.now());
    if (counter < 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "动态验证码不正确，请重试");
    }
    List<String> recoveryCodes = generateRecoveryCodes();
    pending.setConfirmed(true);
    pending.setLastUsedCounter(counter);
    pending.setRecoveryHashes(encodeRecoveryHashes(recoveryCodes));
    pending.setConfirmedAt(Instant.now());
    userTotpRepository.save(pending);
    log.info("TOTP binding confirmed for user {}", userId);
    return recoveryCodes;
  }

  /**
   * 登录第二因子校验：输入 6 位数字走 TOTP，8 位字母数字走还原码。
   * 校验通过后推进防重放计数器（还原码使用即焚）。
   */
  @Transactional
  public void verifyChallenge(Long userId, String code) {
    UserTotp binding = userTotpRepository.findById(userId)
        .filter(UserTotp::isConfirmed)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
            "该账号尚未绑定二步验证，请重新登录以完成绑定"));
    String trimmed = code == null ? "" : code.trim();
    if (trimmed.matches("\\d{6}")) {
      long counter = totpService.verify(binding.getSecret(), trimmed, Instant.now());
      if (counter < 0) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "动态验证码不正确，请重试");
      }
      if (binding.getLastUsedCounter() != null && counter <= binding.getLastUsedCounter()) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "该验证码已被使用，请等待下一个 30 秒窗口");
      }
      binding.setLastUsedCounter(counter);
      userTotpRepository.save(binding);
      return;
    }
    if (trimmed.matches("[A-Za-z0-9]{8}")) {
      consumeRecoveryCode(binding, trimmed.toUpperCase());
      return;
    }
    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请输入 6 位动态验证码或 8 位还原码");
  }

  /** 管理员重置指定用户的 2FA 绑定：下次登录（开关开启时）将重新进入强制绑定。 */
  @Transactional
  public boolean resetBinding(Long userId) {
    if (!userTotpRepository.existsById(userId)) {
      return false;
    }
    userTotpRepository.deleteById(userId);
    log.info("TOTP binding reset by admin for user {}", userId);
    return true;
  }

  // ==================== 还原码 ====================

  private List<String> generateRecoveryCodes() {
    List<String> codes = new ArrayList<>(RECOVERY_CODE_COUNT);
    for (int i = 0; i < RECOVERY_CODE_COUNT; i++) {
      StringBuilder code = new StringBuilder(RECOVERY_CODE_LENGTH);
      for (int j = 0; j < RECOVERY_CODE_LENGTH; j++) {
        code.append(RECOVERY_ALPHABET.charAt(RANDOM.nextInt(RECOVERY_ALPHABET.length())));
      }
      codes.add(code.toString());
    }
    return codes;
  }

  private String encodeRecoveryHashes(List<String> plainCodes) {
    List<String> hashes = new ArrayList<>(plainCodes.size());
    for (String code : plainCodes) {
      hashes.add(passwordEncoder.encode(code));
    }
    try {
      return MAPPER.writeValueAsString(hashes);
    } catch (Exception exception) {
      throw new IllegalStateException("还原码序列化失败", exception);
    }
  }

  private void consumeRecoveryCode(UserTotp binding, String code) {
    List<String> hashes = parseHashes(binding);
    for (int i = 0; i < hashes.size(); i++) {
      if (passwordEncoder.matches(code, hashes.get(i))) {
        hashes.remove(i);
        binding.setRecoveryHashes(writeHashes(hashes));
        userTotpRepository.save(binding);
        log.info("TOTP recovery code consumed for user {}", binding.getUserId());
        return;
      }
    }
    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "还原码不正确或已被使用");
  }

  private List<String> parseHashes(UserTotp binding) {
    if (!StringUtils.hasText(binding.getRecoveryHashes())) {
      return new ArrayList<>();
    }
    try {
      return MAPPER.readValue(binding.getRecoveryHashes(), new TypeReference<List<String>>() {
      });
    } catch (Exception exception) {
      return new ArrayList<>();
    }
  }

  private String writeHashes(List<String> hashes) {
    try {
      return MAPPER.writeValueAsString(hashes);
    } catch (Exception exception) {
      throw new IllegalStateException("还原码序列化失败", exception);
    }
  }

  /** 绑定流程的中间产物：密钥与 otpauth URI（以未确认绑定落库，确认后才可用于登录）。 */
  public record TotpSetup(String secret, String otpauthUri, boolean reused) {
  }
}

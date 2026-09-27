package com.chenxi.astrnest.security.totp;

import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

/**
 * TOTP 动态口令（RFC 6238）——手写实现，零第三方依赖。
 *
 * <p>算法口径：</p>
 * <ul>
 *   <li>HMAC-SHA1（RFC 4226 HOTP 底座），密钥为 20 字节随机数经 Base32 编码后的字符串；</li>
 *   <li>时间步长 30 秒，计数器 C = floor(unixSeconds / 30)；</li>
 *   <li>默认输出 6 位十进制码（动态截断取模 10^6，前导零保留）；</li>
 *   <li>校验窗口 ±1 步（即接受前后 30 秒内的码，应对客户端时钟漂移）；</li>
 *   <li>时间比较使用常量时间比对，避免时序侧信道。</li>
 * </ul>
 *
 * <p>与 RFC 6238 附录 B 测试向量的一致性由 {@code TotpServiceTest} 保证
 * （密钥 "12345678901234567890"（ASCII），SHA1）。</p>
 */
@Service
public class TotpService {

  public static final int TIME_STEP_SECONDS = 30;
  public static final int CODE_DIGITS = 6;
  /** 校验窗口：±1 个时间步 */
  public static final int ALLOWED_DRIFT_STEPS = 1;
  /** 密钥字节数：20 字节（160 位，与 Google Authenticator 推荐一致） */
  public static final int SECRET_BYTES = 20;
  private static final String HMAC_SHA1 = "HmacSHA1";

  /** 生成新的 TOTP 密钥：20 字节安全随机数 → Base32（无填充）。 */
  public String generateSecret() {
    byte[] secret = new byte[SECRET_BYTES];
    java.security.SecureRandom random = new java.security.SecureRandom();
    random.nextBytes(secret);
    return Base32.encode(secret);
  }

  /**
   * 构造 otpauth URI 供验证器扫码：
   * {@code otpauth://totp/{站点名}:{用户名}?secret=..&issuer=AstrNest}
   */
  public String buildOtpauthUri(String secret, String accountName) {
    String label = "AstrNest:" + (accountName == null ? "user" : accountName);
    return "otpauth://totp/" + urlEncode(label)
        + "?secret=" + secret
        + "&issuer=AstrNest"
        + "&algorithm=SHA1"
        + "&digits=" + CODE_DIGITS
        + "&period=" + TIME_STEP_SECONDS;
  }

  /** 以当前时间生成动态码（服务端对时使用，测试也用它做已知密钥的向量断言）。 */
  public String currentCode(String base32Secret, Instant now) {
    long counter = counterAt(now);
    return hotp(base32Secret, counter);
  }

  /** 极简 percent 编码（label 中的保留字符），避免引入额外依赖。 */
  private static String urlEncode(String value) {
    StringBuilder result = new StringBuilder(value.length());
    for (byte item : value.getBytes(java.nio.charset.StandardCharsets.UTF_8)) {
      char c = (char) (item & 0xFF);
      if (Character.isLetterOrDigit(c) || c == '-' || c == '.' || c == '_' || c == '~') {
        result.append(c);
      } else {
        result.append('%').append(String.format("%02X", (int) c));
      }
    }
    return result.toString();
  }

  /**
   * 校验动态码：允许 ±{@value ALLOWED_DRIFT_STEPS} 步时钟漂移。
   *
   * @return 命中的计数器（用于防重放），校验失败返回 -1
   */
  public long verify(String base32Secret, String code, Instant now) {
    if (base32Secret == null || code == null || !code.trim().matches("\\d{" + CODE_DIGITS + "}")) {
      return -1;
    }
    String normalized = code.trim();
    long current = counterAt(now);
    for (int drift = -ALLOWED_DRIFT_STEPS; drift <= ALLOWED_DRIFT_STEPS; drift++) {
      String expected = hotp(base32Secret, current + drift);
      if (constantTimeEquals(expected, normalized)) {
        return current + drift;
      }
    }
    return -1;
  }

  /** RFC 4226 HOTP：HMAC-SHA1 → 动态截断 → 取模 10^digits。 */
  private String hotp(String base32Secret, long counter) {
    byte[] key = Base32.decode(base32Secret);
    byte[] message = ByteBuffer.allocate(8).putLong(counter).array();
    Mac mac;
    try {
      mac = Mac.getInstance(HMAC_SHA1);
      mac.init(new SecretKeySpec(key, HMAC_SHA1));
    } catch (NoSuchAlgorithmException | InvalidKeyException exception) {
      throw new IllegalStateException("TOTP HMAC-SHA1 初始化失败", exception);
    }
    byte[] hash = mac.doFinal(message);
    int offset = hash[hash.length - 1] & 0x0F;
    int binary = ((hash[offset] & 0x7F) << 24)
        | ((hash[offset + 1] & 0xFF) << 16)
        | ((hash[offset + 2] & 0xFF) << 8)
        | (hash[offset + 3] & 0xFF);
    int modulus = (int) Math.pow(10, CODE_DIGITS);
    return String.format("%0" + CODE_DIGITS + "d", binary % modulus);
  }

  private long counterAt(Instant now) {
    return Math.floorDiv(now.getEpochSecond(), TIME_STEP_SECONDS);
  }

  /** 常量时间字符串比较：长度不同立即失败，长度相同则逐位累计异或。 */
  private boolean constantTimeEquals(String expected, String actual) {
    if (expected.length() != actual.length()) {
      return false;
    }
    int result = 0;
    for (int i = 0; i < expected.length(); i++) {
      result |= expected.charAt(i) ^ actual.charAt(i);
    }
    return result == 0;
  }
}

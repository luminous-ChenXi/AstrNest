package com.chenxi.astrnest.security.totp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * TOTP（RFC 6238）一致性测试：使用 RFC 6238 附录 B 的标准测试向量
 * （密钥 = ASCII "12345678901234567890"，即 31323334353637383930
 * 31 32 33 34 35 36 37 38 39 30，HMAC-SHA1）。
 *
 * <p>附录 B 给出的是 8 位动态码；本实现输出 6 位（RFC 4226 推荐的验证器默认口径），
 * 因此期望值取 8 位向量的低 6 位（动态截断模 10^6 的数学结果），换算关系由 RFC 定义、非自造。</p>
 */
class TotpServiceTest {

  private static final String RFC_KEY_ASCII = "12345678901234567890";

  private final TotpService totpService = new TotpService();

  /** RFC 6238 附录 B（SHA1）：T（秒）→ 8 位 TOTP。 */
  private String sixDigitOf(String eightDigit) {
    return eightDigit.substring(eightDigit.length() - 6);
  }

  @Test
  @DisplayName("RFC 6238 附录 B 标准测试向量（T=59/1111111109/1111111111/1234567890/2000000000/20000000000）")
  void matchesRfc6238AppendixBVectors() {
    String base32Key = Base32.encode(RFC_KEY_ASCII.getBytes());
    String[][] vectors = {
        // { unix 秒, RFC 8 位期望值 }
        {"59", "94287082"},
        {"1111111109", "07081804"},
        {"1111111111", "14050471"},
        {"1234567890", "89005924"},
        {"2000000000", "69279037"},
        {"20000000000", "65353130"},
    };
    for (String[] vector : vectors) {
      Instant now = Instant.ofEpochSecond(Long.parseLong(vector[0]));
      String expected6 = sixDigitOf(vector[1]);
      String actual = totpService.currentCode(base32Key, now);
      assertThat(actual)
          .as("T=%s 应生成 6 位码 %s（8 位向量 %s 的低 6 位）", vector[0], expected6, vector[1])
          .isEqualTo(expected6);
      assertThat(totpService.verify(base32Key, expected6, now))
          .as("T=%s 校验应命中当前时间步", vector[0])
          .isEqualTo(now.getEpochSecond() / TotpService.TIME_STEP_SECONDS);
    }
  }

  @Test
  @DisplayName("±1 时间步容差：上一窗/下一窗的码可通过，±2 窗外的码被拒绝")
  void allowsOneStepDriftOnly() {
    String base32Key = Base32.encode(RFC_KEY_ASCII.getBytes());
    long step = TotpService.TIME_STEP_SECONDS;
    Instant now = Instant.ofEpochSecond(1111111109L);

    String previousCode = totpService.currentCode(base32Key, now.minusSeconds(step));
    String nextCode = totpService.currentCode(base32Key, now.plusSeconds(step));
    String farPastCode = totpService.currentCode(base32Key, now.minusSeconds(2L * step));
    String farFutureCode = totpService.currentCode(base32Key, now.plusSeconds(2L * step));

    assertThat(totpService.verify(base32Key, previousCode, now)).isGreaterThanOrEqualTo(0);
    assertThat(totpService.verify(base32Key, nextCode, now)).isGreaterThanOrEqualTo(0);
    assertThat(totpService.verify(base32Key, farPastCode, now)).isEqualTo(-1);
    assertThat(totpService.verify(base32Key, farFutureCode, now)).isEqualTo(-1);
  }

  @Test
  @DisplayName("错误码 / 非法输入被拒绝")
  void rejectsInvalidInputs() {
    String base32Key = Base32.encode(RFC_KEY_ASCII.getBytes());
    Instant now = Instant.ofEpochSecond(59);

    assertThat(totpService.verify(base32Key, "000000", now)).isEqualTo(-1);
    assertThat(totpService.verify(base32Key, null, now)).isEqualTo(-1);
    assertThat(totpService.verify(base32Key, "12345", now)).isEqualTo(-1);
    assertThat(totpService.verify(base32Key, "abcdef", now)).isEqualTo(-1);
    assertThat(totpService.verify(null, "287082", now)).isEqualTo(-1);
  }

  @Test
  @DisplayName("generateSecret：Base32 口径（20 字节 → 32 字符无填充），且每次不同")
  void generatesBase32Secret() {
    String secret = totpService.generateSecret();
    assertThat(secret).hasSize(32);
    assertThat(secret).matches("^[A-Z2-7]+$");
    assertThat(totpService.generateSecret()).isNotEqualTo(secret);
    // 生成的密钥可直接解码回 20 字节
    assertThat(Base32.decode(secret)).hasSize(TotpService.SECRET_BYTES);
  }

  @Test
  @DisplayName("otpauth URI：otpauth://totp/AstrNest:{user}?secret=..&issuer=AstrNest")
  void buildsOtpauthUri() {
    String uri = totpService.buildOtpauthUri("JBSWY3DPEHPK3PXP", "admin");
    assertThat(uri).startsWith("otpauth://totp/AstrNest%3Aadmin?secret=JBSWY3DPEHPK3PXP");
    assertThat(uri).contains("issuer=AstrNest").contains("digits=6").contains("period=30");
  }

  @Test
  @DisplayName("Base32：编解码往返 + 非法字符拒绝")
  void base32RoundTrip() {
    byte[] data = new byte[] {0, 1, 2, (byte) 250, (byte) 251, 127, 64, 32, 16, 8};
    String encoded = Base32.encode(data);
    assertThat(encoded).matches("^[A-Z2-7]+$");
    assertThat(Base32.decode(encoded)).isEqualTo(data);
    // 小写、空格与填充符应被宽容处理
    assertThat(Base32.decode(encoded.toLowerCase().replace("", " ").trim())).isEqualTo(data);
    assertThatThrownBy(() -> Base32.decode("01")).isInstanceOf(IllegalArgumentException.class);
  }
}

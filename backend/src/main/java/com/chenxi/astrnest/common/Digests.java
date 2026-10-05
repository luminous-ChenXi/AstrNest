package com.chenxi.astrnest.common;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 摘要工具：邮箱验证码/注册链接令牌/图形验证码入库前做 SHA-256——
 * 数据库泄露不再等于「可直接使用的凭证」；比对时对入参做同样哈希后用
 * {@link MessageDigest#isEqual} 恒定时间比较。
 */
public final class Digests {

  private Digests() {
  }

  public static String sha256Hex(String value) {
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256")
          .digest((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
      StringBuilder builder = new StringBuilder(digest.length * 2);
      for (byte b : digest) {
        builder.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
      }
      return builder.toString();
    } catch (NoSuchAlgorithmException exception) {
      // JVM 必带 SHA-256，不可能走到这里
      throw new IllegalStateException(exception);
    }
  }

  /** 恒定时间比较（防时序侧信道），任一侧为 null 视为空串 */
  public static boolean constantTimeEquals(String left, String right) {
    return MessageDigest.isEqual(
        (left == null ? "" : left).getBytes(StandardCharsets.UTF_8),
        (right == null ? "" : right).getBytes(StandardCharsets.UTF_8));
  }
}

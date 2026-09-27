package com.chenxi.astrnest.security.totp;

import java.util.Arrays;

/**
 * RFC 4648 Base32 编解码（A-Z、2-9，无填充小写不合法），仅服务于 TOTP 密钥的存取。
 *
 * <p>手写实现避免引入外部依赖（ commons-codec / Guava 等），
 * 编码表与 RFC 4648 完全一致，Google Authenticator 等验证器可直接识别。</p>
 */
public final class Base32 {

  private static final char[] ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".toCharArray();
  private static final int[] LOOKUP = new int[128];

  static {
    Arrays.fill(LOOKUP, -1);
    for (int i = 0; i < ALPHABET.length; i++) {
      LOOKUP[ALPHABET[i]] = i;
    }
  }

  private Base32() {
  }

  /** 编码：输出不含填充（'='）的标准 Base32 大写串。 */
  public static String encode(byte[] data) {
    StringBuilder result = new StringBuilder((data.length * 8 + 4) / 5);
    int buffer = 0;
    int bits = 0;
    for (byte item : data) {
      buffer = (buffer << 8) | (item & 0xFF);
      bits += 8;
      while (bits >= 5) {
        result.append(ALPHABET[(buffer >>> (bits - 5)) & 0x1F]);
        bits -= 5;
      }
    }
    if (bits > 0) {
      result.append(ALPHABET[(buffer << (5 - bits)) & 0x1F]);
    }
    return result.toString();
  }

  /** 解码：忽略大小写、空白与填充符；非法字符抛 IllegalArgumentException。 */
  public static byte[] decode(String encoded) {
    String normalized = encoded.trim().replace("=", "").replace(" ", "").toUpperCase();
    if (normalized.isEmpty()) {
      return new byte[0];
    }
    ByteArrayOutputStreamBuffer out = new ByteArrayOutputStreamBuffer(normalized.length() * 5 / 8 + 1);
    int buffer = 0;
    int bits = 0;
    for (int i = 0; i < normalized.length(); i++) {
      char c = normalized.charAt(i);
      if (c >= 128 || LOOKUP[c] < 0) {
        throw new IllegalArgumentException("非法 Base32 字符: " + c);
      }
      buffer = (buffer << 5) | LOOKUP[c];
      bits += 5;
      if (bits >= 8) {
        out.write((buffer >>> (bits - 8)) & 0xFF);
        bits -= 8;
      }
    }
    return out.toByteArray();
  }

  /** 轻量字节数组输出（避免每次扩容拷贝 java.io.ByteArrayOutputStream 的同步开销，语义一致）。 */
  private static final class ByteArrayOutputStreamBuffer {
    private byte[] data;
    private int count;

    private ByteArrayOutputStreamBuffer(int size) {
      this.data = new byte[Math.max(8, size)];
    }

    private void write(int value) {
      if (count == data.length) {
        data = Arrays.copyOf(data, data.length * 2);
      }
      data[count++] = (byte) value;
    }

    private byte[] toByteArray() {
      return Arrays.copyOf(data, count);
    }
  }
}

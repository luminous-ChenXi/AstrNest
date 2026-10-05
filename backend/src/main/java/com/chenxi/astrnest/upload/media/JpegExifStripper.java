package com.chenxi.astrnest.upload.media;

import java.io.ByteArrayOutputStream;

/**
 * JPEG 元数据剥离（审计 P3 隐私项）：上传图片常携带 EXIF——GPS 定位、拍摄设备、
 * 序列号、内嵌缩略图原图等，直链对外发布即泄露。这里在存储前做**标记段级**剥离：
 * 只删 APP1（EXIF/XMP）、APP13（Photoshop/IPTC）与 COM（注释），保留 APP0（JFIF）与
 * APP2（ICC 色彩配置），扫描数据原样拷贝——不重编码、无画质损失。
 *
 * <p>任何解析异常一律 fail-open 返回原始字节：隐私剥离是尽力而为，绝不阻断上传。</p>
 */
public final class JpegExifStripper {

  private JpegExifStripper() {
  }

  /** 非 JPEG 或未发现需剥离的段时返回原数组引用（零拷贝） */
  public static byte[] strip(byte[] jpeg) {
    if (jpeg == null || jpeg.length < 4
        || (jpeg[0] & 0xFF) != 0xFF || (jpeg[1] & 0xFF) != 0xD8) {
      return jpeg;
    }
    ByteArrayOutputStream out = new ByteArrayOutputStream(jpeg.length);
    out.write(jpeg, 0, 2);
    boolean dropped = false;
    int i = 2;
    try {
      while (i + 4 <= jpeg.length) {
        if ((jpeg[i] & 0xFF) != 0xFF) {
          // 标记流错位：无法安全继续，返回原字节
          return jpeg;
        }
        int marker = jpeg[i + 1] & 0xFF;
        if (marker == 0xD8 || marker == 0x01 || (marker >= 0xD0 && marker <= 0xD7)) {
          // 独立标记（无长度域）：原样拷贝
          out.write(jpeg, i, 2);
          i += 2;
          continue;
        }
        if (marker == 0xDA) {
          // SOS：其后是熵编码数据，原样拷贝到结尾；全程无丢弃则零拷贝返回原数组
          if (!dropped) {
            return jpeg;
          }
          out.write(jpeg, i, jpeg.length - i);
          return out.toByteArray();
        }
        if (marker == 0xD9) {
          // EOI：原样拷贝并结束
          if (!dropped) {
            return jpeg;
          }
          out.write(jpeg, i, jpeg.length - i);
          return out.toByteArray();
        }
        int length = ((jpeg[i + 2] & 0xFF) << 8) | (jpeg[i + 3] & 0xFF);
        if (length < 2 || i + 2 + length > jpeg.length) {
          return jpeg;
        }
        int segmentTotal = 2 + length;
        if (marker == 0xE1 || marker == 0xED || marker == 0xFE) {
          // APP1（EXIF/XMP）、APP13（IPTC/Photoshop）、COM（注释）：整段丢弃
          dropped = true;
          i += segmentTotal;
          continue;
        }
        out.write(jpeg, i, segmentTotal);
        i += segmentTotal;
      }
    } catch (Exception ignored) {
      return jpeg;
    }
    // 走到文件尾都没有 SOS/EOI：截断的 JPEG，返回原字节（媒体校验层另行处理）
    return dropped ? out.toByteArray() : jpeg;
  }
}

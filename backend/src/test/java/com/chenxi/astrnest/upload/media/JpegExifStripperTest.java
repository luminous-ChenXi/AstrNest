package com.chenxi.astrnest.upload.media;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class JpegExifStripperTest {

  /** 构造带 APP0/APP1(EXIF+GPS)/APP2(ICC)/COM/SOS 的合成 JPEG */
  private byte[] syntheticJpeg() {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    out.write(0xFF);
    out.write(0xD8); // SOI
    out.write(0xFF);
    out.write(0xE0); // APP0 JFIF
    out.write(0x00);
    out.write(0x10);
    out.write("JFIF\0".getBytes(), 0, 5);
    out.write(new byte[]{0x01, 0x02, 0x00, 0x00, 0x01, 0x00, 0x01, 0x00, 0x00}, 0, 9);
    byte[] exifPayload = "Exif\0\0II*\0GPSLatitude: 31.23 N, Serial: X99".getBytes();
    out.write(0xFF);
    out.write(0xE1); // APP1 EXIF
    out.write((exifPayload.length + 2) >> 8);
    out.write((exifPayload.length + 2) & 0xFF);
    out.write(exifPayload, 0, exifPayload.length);
    byte[] icc = "ICC_PROFILE".getBytes();
    out.write(0xFF);
    out.write(0xE2); // APP2 ICC（应保留）
    out.write((icc.length + 2) >> 8);
    out.write((icc.length + 2) & 0xFF);
    out.write(icc, 0, icc.length);
    byte[] comment = "Made by ChenXi phone".getBytes();
    out.write(0xFF);
    out.write(0xFE); // COM（应丢弃）
    out.write((comment.length + 2) >> 8);
    out.write((comment.length + 2) & 0xFF);
    out.write(comment, 0, comment.length);
    out.write(0xFF);
    out.write(0xDA); // SOS
    out.write(new byte[]{0x00, 0x02, 0x11, 0x00}, 0, 4);
    out.write(0x12);
    out.write(0x34);
    out.write(0x56);
    out.write(0xFF);
    out.write(0xD9); // EOI
    return out.toByteArray();
  }

  @Test
  void stripsExifAndCommentButKeepsJfifIccAndScanData() {
    byte[] original = syntheticJpeg();
    byte[] stripped = JpegExifStripper.strip(original);

    String asText = new String(stripped);
    assertThat(asText).doesNotContain("GPSLatitude").doesNotContain("Made by ChenXi");
    assertThat(asText).contains("JFIF").contains("ICC_PROFILE");
    // 扫描数据与 EOI 原样保留
    assertThat(stripped[stripped.length - 2]).isEqualTo((byte) 0xFF);
    assertThat(stripped[stripped.length - 1]).isEqualTo((byte) 0xD9);
    assertThat(stripped.length).isLessThan(original.length);
  }

  @Test
  void cleanJpegIsReturnedByReference() {
    byte[] original = syntheticJpeg();
    // 去掉 APP1 与 COM 的干净版本：直接对已剥离结果再剥一次应返回同一引用
    byte[] once = JpegExifStripper.strip(original);
    byte[] twice = JpegExifStripper.strip(once);
    assertThat(twice).isSameAs(once);
  }

  @Test
  void nonJpegIsUntouched() {
    byte[] png = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    assertThat(JpegExifStripper.strip(png)).isSameAs(png);
    assertThat(JpegExifStripper.strip(null)).isNull();
    byte[] junk = {1, 2, 3};
    assertThat(JpegExifStripper.strip(junk)).isSameAs(junk);
  }

  @Test
  void malformedSegmentLengthFailsOpen() {
    byte[] truncated = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE1, 0x00, 0x7F, 0x01, 0x02};
    assertThat(JpegExifStripper.strip(truncated)).isSameAs(truncated);
  }

  @Test
  void strippedFileStillPassesMediaInspection() {
    byte[] stripped = JpegExifStripper.strip(syntheticJpeg());
    MockMultipartFile file = new MockMultipartFile(
        "files", "photo.jpg", "image/jpeg", stripped);
    ChenxiMediaInspector inspector = new ChenxiMediaInspector();
    assertThat(inspector.inspect(file).category()).isEqualTo(MediaCategory.IMAGE);
  }
}

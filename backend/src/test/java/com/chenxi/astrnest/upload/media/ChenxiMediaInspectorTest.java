package com.chenxi.astrnest.upload.media;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

class ChenxiMediaInspectorTest {

  private final ChenxiMediaInspector inspector = new ChenxiMediaInspector();

  @Test
  void acceptsPlainSvg() {
    MockMultipartFile file = new MockMultipartFile("files", "a.svg", "image/svg+xml",
        "<svg xmlns=\"http://www.w3.org/2000/svg\"></svg>".getBytes(StandardCharsets.UTF_8));
    assertThat(inspector.inspect(file).category()).isEqualTo(MediaCategory.IMAGE);
  }

  @Test
  void acceptsSvgWithXmlDeclaration() {
    String content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><svg xmlns=\"http://www.w3.org/2000/svg\"></svg>";
    MockMultipartFile file = new MockMultipartFile("files", "b.svg", "image/svg+xml",
        content.getBytes(StandardCharsets.UTF_8));
    assertThat(inspector.inspect(file).category()).isEqualTo(MediaCategory.IMAGE);
  }

  @Test
  void rejectsHtmlDisguisedAsSvg() {
    MockMultipartFile file = new MockMultipartFile("files", "c.svg", "image/svg+xml",
        "<html><body>hi</body></html>".getBytes(StandardCharsets.UTF_8));
    assertThatThrownBy(() -> inspector.inspect(file)).isInstanceOf(ResponseStatusException.class);
  }

  @Test
  void rejectsForbiddenScriptExtension() {
    MockMultipartFile file = new MockMultipartFile("files", "evil.sh", "application/x-sh",
        "#!/bin/sh\n".getBytes(StandardCharsets.UTF_8));
    assertThatThrownBy(() -> inspector.inspect(file)).isInstanceOf(ResponseStatusException.class);
  }

  @Test
  void rejectsExtensionMismatchWithDeclaredType() {
    // MIME 声明为视频、扩展名是 png：交叉校验必须拒绝，防止 Content-Type 混淆
    MockMultipartFile file = new MockMultipartFile("files", "x.png", "video/mp4",
        new byte[]{0, 0, 0, 24, 'f', 't', 'y', 'p', 'i', 's', 'o', 'm'});
    assertThatThrownBy(() -> inspector.inspect(file)).isInstanceOf(ResponseStatusException.class);
  }

  @Test
  void acceptsPngByMagicBytes() {
    byte[] png = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 13};
    MockMultipartFile file = new MockMultipartFile("files", "ok.png", "image/png", png);
    assertThat(inspector.inspect(file).category()).isEqualTo(MediaCategory.IMAGE);
  }
}

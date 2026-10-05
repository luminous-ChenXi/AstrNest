package com.chenxi.astrnest.storage;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class StorageObjectKeysTest {

  @Test
  void datedPrefixIsYearSlashMonth() {
    assertThat(StorageObjectKeys.datedPrefix()).matches("\\d{4}/\\d{2}");
  }

  @Test
  void keepsSaneExtensionFromOriginalName() {
    MockMultipartFile file = new MockMultipartFile("files", "photo.JPG", "image/jpeg", new byte[]{1});
    String name = StorageObjectKeys.randomFileName(file);
    // 随机命名：不保留原始文件名本体，只保留安全扩展名
    assertThat(name).endsWith(".jpg").doesNotContain("photo");
  }

  @Test
  void fallsBackToContentTypeSubtype() {
    MockMultipartFile file = new MockMultipartFile("files", "blob", "image/png", new byte[]{1});
    assertThat(StorageObjectKeys.randomFileName(file)).endsWith(".png");
  }

  @Test
  void stripsComplexSubtypeLikeSvgXml() {
    MockMultipartFile file = new MockMultipartFile("files", "blob", "image/svg+xml", new byte[]{1});
    assertThat(StorageObjectKeys.randomFileName(file)).endsWith(".svg");
  }

  @Test
  void refusesUnsafeOriginalExtensionAndFallsBack() {
    // 扩展名带空格等异常字符：不能进对象 key，回退 Content-Type 推断
    MockMultipartFile file = new MockMultipartFile("files", "x.jpg ", "image/jpeg", new byte[]{1});
    String name = StorageObjectKeys.randomFileName(file);
    assertThat(name).endsWith(".jpeg").doesNotContain(" ");
  }

  @Test
  void generatesUniqueNames() {
    Set<String> seen = new HashSet<>();
    for (int i = 0; i < 100; i++) {
      seen.add(StorageObjectKeys.randomFileName(
          new MockMultipartFile("files", "a.png", "image/png", new byte[]{1})));
    }
    assertThat(seen).hasSize(100);
  }
}

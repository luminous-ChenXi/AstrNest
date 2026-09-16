package com.chenxi.astrnest.storage;

import java.nio.file.Path;
import org.springframework.util.StringUtils;

/**
 * 存储对象键校验工具：防止路径穿越（../、绝对路径、反斜杠逃逸等）。
 *
 * <p>所有把外部传入的 objectKey 解析为文件系统路径的地方都必须先经过本工具校验。
 * 校验失败时统一抛出 {@link StorageObjectNotFoundException}，对外表现为 404，不泄露任何路径信息。
 */
public final class StoragePathGuard {

  private StoragePathGuard() {
  }

  /**
   * 校验 objectKey 是否为安全的相对键：非空、不含 ".." 路径段、不以 "/" 或 "\" 开头、
   * 不包含反斜杠或 NUL 字符、不包含 Windows 盘符前缀。
   * 不合格时抛出 {@link StorageObjectNotFoundException}。
   */
  public static void assertSafeObjectKey(String objectKey) {
    if (!StringUtils.hasText(objectKey)) {
      throw new StorageObjectNotFoundException("");
    }
    String key = objectKey.trim();
    if (key.contains("\u0000")
        || key.contains("\\")
        || key.startsWith("/")
        || key.contains(":")
        || key.equals(".")
        || key.equals("..")
        || key.contains("/../")
        || key.endsWith("/..")
        || key.startsWith("../")
        || key.contains("/./")
        || key.endsWith("/.")
        || key.startsWith("./")) {
      throw new StorageObjectNotFoundException(objectKey);
    }
  }

  /**
   * 将 objectKey 解析到 root 之下并校验：resolve → normalize → startsWith(root)。
   * 校验失败（试图逃逸 root）时抛出 {@link StorageObjectNotFoundException}。
   *
   * @return 解析并规范化后、确认位于 root 之内的目标路径
   */
  public static Path assertInsideRoot(Path root, String objectKey) {
    if (root == null) {
      throw new StorageObjectNotFoundException("");
    }
    assertSafeObjectKey(objectKey);
    Path normalizedRoot = root.toAbsolutePath().normalize();
    Path resolved = normalizedRoot.resolve(objectKey.trim()).normalize();
    if (!resolved.startsWith(normalizedRoot) || resolved.equals(normalizedRoot)) {
      throw new StorageObjectNotFoundException(objectKey);
    }
    return resolved;
  }
}

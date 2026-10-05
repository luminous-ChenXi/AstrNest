package com.chenxi.astrnest.storage;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * 对象 key 生成工具：所有存储后端（本地/云）统一使用「日期目录 + 随机文件名」。
 *
 * <p>历史上 objectKey 使用「年/月/原始文件名」：对象存储 put 为覆盖语义，任何用户上传同名文件
 * 即可覆盖他人当月对象；同时 key 可被枚举猜测，"私密"图直链形同虚设。随机文件名同时消除这两个面，
 * 原始文件名始终保留在 upload_records.original_name 与 API 响应中用于展示。</p>
 */
public final class StorageObjectKeys {

  /** 扩展名口径收紧为安全字符集，防止把路径分隔符/控制字符等异常内容拼进对象 key */
  private static final Pattern SAFE_EXTENSION = Pattern.compile("^\\.[a-z0-9]{1,10}$");
  private static final Pattern SAFE_SUBTYPE = Pattern.compile("^[a-z0-9]{1,10}$");

  private StorageObjectKeys() {
  }

  /** "yyyy/MM" 形式的日期目录段（系统默认时区） */
  public static String datedPrefix() {
    ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
    return String.format("%04d/%02d", now.getYear(), now.getMonthValue());
  }

  /** 随机文件名（UUID + 扩展名）：扩展名优先取原始文件名的安全后缀，其次按 Content-Type 推断 */
  public static String randomFileName(MultipartFile file) {
    return UUID.randomUUID() + resolveExtension(file);
  }

  private static String resolveExtension(MultipartFile file) {
    String original = file != null ? file.getOriginalFilename() : null;
    if (StringUtils.hasText(original)) {
      String cleaned = original.replace('\\', '/');
      int slash = cleaned.lastIndexOf('/');
      String name = slash >= 0 ? cleaned.substring(slash + 1) : cleaned;
      int dot = name.lastIndexOf('.');
      if (dot >= 0 && dot < name.length() - 1) {
        String ext = name.substring(dot).toLowerCase(Locale.ROOT);
        if (SAFE_EXTENSION.matcher(ext).matches()) {
          return ext;
        }
      }
    }
    String contentType = file != null ? file.getContentType() : null;
    if (StringUtils.hasText(contentType) && contentType.contains("/")) {
      // image/svg+xml → svg；application/octet-stream 等非安全字符集一律不给扩展名
      String subtype = contentType.substring(contentType.indexOf('/') + 1).trim().toLowerCase(Locale.ROOT);
      int plus = subtype.indexOf('+');
      if (plus > 0) {
        subtype = subtype.substring(0, plus);
      }
      if (SAFE_SUBTYPE.matcher(subtype).matches()) {
        return "." + subtype;
      }
    }
    return "";
  }
}

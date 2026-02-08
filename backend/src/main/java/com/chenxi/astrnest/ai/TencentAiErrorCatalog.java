package com.chenxi.astrnest.ai;

import java.util.Map;
import java.util.Optional;

public final class TencentAiErrorCatalog {

  private static final Map<String, String> CODE_HINTS = Map.ofEntries(
      Map.entry("ImageTooLarge", "图片文件超过腾讯云限制，请压缩后重试"),
      Map.entry("ImageResolutionExceed", "图片分辨率超限，建议控制在 9999x9999 以内"),
      Map.entry("InvalidArgument", "请求参数不合法，请检查 detect-url、ObjectKey 等参数"),
      Map.entry("InvalidImageFormat", "图片格式暂不支持，可改为 jpg/png/webp 等常用格式"),
      Map.entry("AccessDenied", "密钥或角色无权访问该 Bucket，请确认已为数据万象开启权限"),
      Map.entry("NoSuchBucket", "目标存储桶不存在，请确认 bucket 名称和地域"),
      Map.entry("SignatureDoesNotMatch", "签名校验失败，请检查 SecretId/SecretKey 是否正确"),
      Map.entry("RequestTimeTooSkewed", "本地时间与服务器差异过大，需校准服务器时间"),
      Map.entry("SlowDown", "调用频率超限，需降低请求频率或申请更高配额"),
      Map.entry("InternalError", "腾讯云侧暂时异常，可稍后重试或携带 RequestId 联系支持"),
      Map.entry("MissingAuthorization", "缺失签名，请补充 Authorization 头或 sts token"),
      Map.entry("AuditingJobNotFound", "审核任务不存在，可能是 JobId 不正确"),
      Map.entry("File is too large", "文件过大，需缩小后再提交"),
      Map.entry("File is too large or empty", "文件体积异常（过大或为空），请重新上传"),
      Map.entry("Resolution is too small", "图片分辨率过低，无法进行审核"),
      Map.entry("Download file failed", "腾讯云无法回源该图片，请确认链接可公网访问"),
      Map.entry("url unreachable", "检测地址不可访问，请确保 detect-url 已做 URL Encode 并可公网下载")
  );

  private TencentAiErrorCatalog() {
  }

  public static Optional<String> friendlyMessage(String code) {
    if (code == null || code.isBlank()) {
      return Optional.empty();
    }
    return Optional.ofNullable(CODE_HINTS.get(code));
  }
}

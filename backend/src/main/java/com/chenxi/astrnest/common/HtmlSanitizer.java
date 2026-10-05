package com.chenxi.astrnest.common;

import java.util.Locale;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;

/**
 * 服务端富文本/URL 清洗（审计 P1-11）：此前所有入库内容的 XSS 防护完全依赖前端 DOMPurify，
 * 任何绕过网页的 API 消费方都会中招。jsoup 白名单在这里补上服务端纵深。
 */
public final class HtmlSanitizer {

  private HtmlSanitizer() {
  }

  /**
   * 本意就是 HTML 的字段（系统 footer 等）：jsoup 白名单过滤，
   * 剥离 script/事件属性/javascript: 协议，保留常用排版标签。
   */
  public static String richText(String html) {
    if (!StringUtils.hasText(html)) {
      return html;
    }
    return Jsoup.clean(html.trim(), "", Safelist.relaxed().preserveRelativeLinks(true));
  }

  /**
   * Markdown 源净化：整体 HTML 转义（纯字符串处理，不解析 HTML——jsoup 的 HTML 解析会把
   * 换行折叠、破坏 Markdown 结构）。转义后 {@code <script>} 变为字面量文本，前端 marked
   * 照常渲染 Markdown 语法；代价是公告里不再支持内嵌原始 HTML。
   */
  public static String markdownSource(String markdown) {
    if (!StringUtils.hasText(markdown)) {
      return markdown;
    }
    return HtmlUtils.htmlEscape(markdown.trim());
  }

  /**
   * 外链 URL 协议白名单：仅放行 http(s) 绝对地址与 "/" 开头站内相对路径，
   * javascript:/data:/vbscript: 等一律返回 null（调用方据此清空字段）。
   */
  public static String externalUrl(String url) {
    if (!StringUtils.hasText(url)) {
      return null;
    }
    String trimmed = url.trim();
    String lower = trimmed.toLowerCase(Locale.ROOT);
    if (lower.startsWith("http://") || lower.startsWith("https://") || trimmed.startsWith("/")) {
      return trimmed;
    }
    return null;
  }
}

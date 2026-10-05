package com.chenxi.astrnest.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class HtmlSanitizerTest {

  @Test
  void markdownEscapesRawHtmlButKeepsMarkdownStructure() {
    String markdown = "# 标题\n**加粗** <script>alert(1)</script>\n- 列表项";
    String result = HtmlSanitizer.markdownSource(markdown);
    assertThat(result)
        .doesNotContain("<script>")
        .contains("&lt;script&gt;alert(1)&lt;/script&gt;")
        .contains("# 标题")
        .contains("**加粗**")
        .contains("- 列表项")
        // Markdown 是逐行渲染的：换行与结构必须原样保留
        .contains("\n");
  }

  @Test
  void markdownKeepsPlainTextUntouched() {
    String markdown = "普通公告内容，无任何标记。";
    assertThat(HtmlSanitizer.markdownSource(markdown)).isEqualTo(markdown);
  }

  @Test
  void richTextStripsScriptAndEventHandlers() {
    String html = "<p onclick=\"evil()\">内容</p><script>alert(1)</script><b>加粗</b>";
    String result = HtmlSanitizer.richText(html);
    assertThat(result)
        .doesNotContain("onclick")
        .doesNotContain("<script")
        .doesNotContain("alert(1)")
        .contains("内容")
        .contains("加粗");
  }

  @Test
  void richTextKeepsCommonTagsAndLinks() {
    String html = "<div><a href=\"https://example.com\">链接</a><img src=\"/img/a.png\"></div>";
    String result = HtmlSanitizer.richText(html);
    assertThat(result).contains("href=\"https://example.com\"").contains("img");
  }

  @Test
  void externalUrlAcceptsHttpsAndRelative() {
    assertThat(HtmlSanitizer.externalUrl("https://cdn.example.com/a.png")).isEqualTo("https://cdn.example.com/a.png");
    assertThat(HtmlSanitizer.externalUrl("http://example.com/x")).isEqualTo("http://example.com/x");
    assertThat(HtmlSanitizer.externalUrl("/assets/avatar.png")).isEqualTo("/assets/avatar.png");
  }

  @Test
  void externalUrlRejectsDangerousSchemes() {
    assertThat(HtmlSanitizer.externalUrl("javascript:alert(1)")).isNull();
    assertThat(HtmlSanitizer.externalUrl("data:text/html;base64,xxx")).isNull();
    assertThat(HtmlSanitizer.externalUrl("  ")).isNull();
    assertThat(HtmlSanitizer.externalUrl(null)).isNull();
  }
}

package com.chenxi.astrnest.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 全局安全响应头加固（对所有响应生效，含错误响应）：
 *
 * <ul>
 *   <li>{@code X-Content-Type-Options: nosniff}：禁止浏览器 MIME 嗅探，防止上传内容被误解析执行</li>
 *   <li>{@code X-Frame-Options: SAMEORIGIN}：禁止跨站 iframe 嵌套。<b>例外：/embed/** 豁免</b>——
 *       前端上传结果页提供「嵌入代码」复制功能（{@code <iframe src="{站点}/embed/video/uuid">}），
 *       该代码面向站外博客/论坛等跨域场景，必须保持可被外部页面嵌入（点击劫持风险低：嵌入页仅视频播放器，
 *       无敏感操作入口）。/upload/** 等媒体直链经 {@code <img>/<video>} 标签消费，XFO 不影响其正常使用</li>
 *   <li>{@code Referrer-Policy: strict-origin-when-cross-origin}：跨站请求仅泄露 origin，不泄露完整 URL</li>
 *   <li>{@code Strict-Transport-Security: max-age=15552000}：HSTS 半年（浏览器在纯 HTTP 下自动忽略该头，无副作用）</li>
 * </ul>
 *
 * <p>注册方式与 {@link MediaServingHeaderFilter} 一致（{@code @Component} + {@code @Order}），
 * 排在其后，保证媒体路径的 CSP sandbox 头不被覆盖（本过滤器不写 Content-Security-Policy）。
 * SecurityConfig 侧 frameOptions 保持 disable，避免 HeaderWriterFilter 以 DENY 覆盖本过滤器的取值；
 * XFO 统一由本过滤器按路径差异化下发。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class GlobalSecurityHeaderFilter extends OncePerRequestFilter {

  private static final String FRAME_EMBED_PATH_PREFIX = "/embed/";

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {
    // 提前写入，确保本过滤器内产生的错误响应同样携带
    response.setHeader("X-Content-Type-Options", "nosniff");
    if (!isEmbedPath(request.getRequestURI())) {
      response.setHeader("X-Frame-Options", "SAMEORIGIN");
    }
    response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
    response.setHeader("Strict-Transport-Security", "max-age=15552000");
    filterChain.doFilter(request, response);
  }

  private boolean isEmbedPath(String requestUri) {
    return requestUri != null && requestUri.startsWith(FRAME_EMBED_PATH_PREFIX);
  }
}

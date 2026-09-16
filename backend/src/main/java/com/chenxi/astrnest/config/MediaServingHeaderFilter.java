package com.chenxi.astrnest.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.util.List;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 媒体文件服务响应头加固：
 *
 * <p>上传的 SVG 会被浏览器按 XML/HTML 混合模式解析，同域 inline 提供时存在存储型 XSS 风险。
 * 本过滤器不禁止 SVG 上传（保持功能不变），而是在服务端中和脚本执行：
 * 对 image/svg+xml 响应追加 {@code Content-Security-Policy: sandbox}，
 * 并为所有媒体响应统一追加 {@code X-Content-Type-Options: nosniff}。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MediaServingHeaderFilter extends OncePerRequestFilter {

  private static final String CSP_SANDBOX = "sandbox";

  private static final List<String> PROTECTED_PREFIXES = List.of(
      "/api/public/assets/",
      "/api/uploads/",
      "/upload/"
  );

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {
    if (!isMediaPath(request.getRequestURI())) {
      filterChain.doFilter(request, response);
      return;
    }

    // 提前写入 nosniff，确保错误响应（如 404/403）同样生效
    response.setHeader("X-Content-Type-Options", "nosniff");

    // 通过包装器在 setContentType 时立即追加 CSP，避免大文件流式写 body 导致响应
    // 已提交（committed）之后再设头失效
    HttpServletResponseWrapper wrapped = new HttpServletResponseWrapper(response) {
      @Override
      public void setContentType(String type) {
        super.setContentType(type);
        applySvgHeaderIfNecessary(type);
      }

      @Override
      public void addHeader(String name, String value) {
        super.addHeader(name, value);
        if (isContentTypeHeader(name)) {
          applySvgHeaderIfNecessary(value);
        }
      }

      @Override
      public void setHeader(String name, String value) {
        super.setHeader(name, value);
        if (isContentTypeHeader(name)) {
          applySvgHeaderIfNecessary(value);
        }
      }

      private boolean isContentTypeHeader(String name) {
        return "Content-Type".equalsIgnoreCase(name);
      }

      private void applySvgHeaderIfNecessary(String contentType) {
        if (StringUtils.hasText(contentType) && contentType.toLowerCase().contains("image/svg+xml")) {
          // CSP sandbox 会禁用 SVG 内的脚本执行（即使内联在 <script> 或事件属性中）
          setHeaderInternal(CSP_SANDBOX);
        }
      }

      private void setHeaderInternal(String value) {
        super.setHeader("Content-Security-Policy", value);
      }
    };

    filterChain.doFilter(request, wrapped);
  }

  private boolean isMediaPath(String requestUri) {
    if (!StringUtils.hasText(requestUri)) {
      return false;
    }
    for (String prefix : PROTECTED_PREFIXES) {
      if (requestUri.startsWith(prefix)) {
        return true;
      }
    }
    return false;
  }
}

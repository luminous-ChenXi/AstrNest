package com.chenxi.astrnest.install;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.DefaultCorsProcessor;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 安装向导守卫过滤器（注册在最高优先级，方式参考 MediaServingHeaderFilter）。
 *
 * <p>未安装（installed=false）时：放行 /api/install/**、/api/system/public-config、
 * /error、静态资源与 /embed/**（这些不依赖业务表）；其余 /api/** 返回 503 JSON
 * （沿用 GlobalExceptionHandler 的 ApiErrorResponse 风格，code=50301），
 * 避免未建表时业务接口抛出难以理解的 500。</p>
 *
 * <p>已安装时：直接放行，/api/install/** 写操作的 403 由 InstallController 层守卫。</p>
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class InstallGuardFilter extends OncePerRequestFilter {

  private static final String BLOCKED_CODE = "50301";
  private static final String BLOCKED_MESSAGE = "系统尚未安装，请先完成安装向导";

  private final InstallStatusService installStatusService;
  private final ObjectMapper objectMapper;
  private final CorsConfigurationSource corsConfigurationSource;
  private final DefaultCorsProcessor corsProcessor = new DefaultCorsProcessor();

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain filterChain) throws jakarta.servlet.ServletException, java.io.IOException {
    String path = request.getRequestURI();
    if (!StringUtils.hasText(path) || !path.startsWith("/api/")) {
      // 静态资源、/embed/**、/error 等不碰数据库，一律放行
      filterChain.doFilter(request, response);
      return;
    }
    if (isAllowedBeforeInstall(path)) {
      filterChain.doFilter(request, response);
      return;
    }
    if (installStatusService.isInstalled()) {
      filterChain.doFilter(request, response);
      return;
    }
    writeBlockedResponse(request, response);
  }

  private boolean isAllowedBeforeInstall(String path) {
    return path.startsWith("/api/install/")
        || "/api/install".equals(path)
        || "/api/system/public-config".equals(path);
  }

  /**
   * 输出 503 JSON。先应用 CORS 配置，保证跨域前端（dev 直连后端端口时）也能读到该响应。
   */
  private void writeBlockedResponse(HttpServletRequest request, HttpServletResponse response)
      throws java.io.IOException {
    try {
      CorsConfiguration corsConfiguration = corsConfigurationSource.getCorsConfiguration(request);
      if (corsConfiguration != null) {
        corsProcessor.processRequest(corsConfiguration, request, response);
      }
    } catch (Exception exception) {
      log.debug("Apply CORS on 503 install response failed: {}", exception.getMessage());
    }
    if (response.isCommitted()) {
      return;
    }
    response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");
    Map<String, Object> body = Map.of(
        "code", BLOCKED_CODE,
        "message", BLOCKED_MESSAGE,
        "status", HttpStatus.SERVICE_UNAVAILABLE.value(),
        "timestamp", Instant.now().toString()
    );
    objectMapper.writeValue(response.getWriter(), body);
  }
}

package com.chenxi.astrnest.security.jwt;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Bearer JWT 认证过滤器：仅当请求携带 {@code Authorization: Bearer <JWT>} 时尝试解析。
 *
 * <p>失败策略与现有 HTTP Basic 一致——无效/过期/用户不存在时不设置认证、不阻断请求，
 * 由后续授权规则返回 401/403。角色经 {@link UserDetailsService} 从数据库实时加载，改角色即刻生效。
 * HTTP Basic 通道不受影响（本过滤器不处理 Basic 头，由 BasicAuthenticationFilter 兜底）。
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private static final String BEARER_PREFIX = "Bearer ";

  private final JwtTokenService jwtTokenService;
  private final UserDetailsService userDetailsService;

  public JwtAuthenticationFilter(JwtTokenService jwtTokenService, UserDetailsService userDetailsService) {
    this.jwtTokenService = jwtTokenService;
    this.userDetailsService = userDetailsService;
  }

  @Override
  protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain) throws ServletException, IOException {
    String header = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (StringUtils.hasText(header)
        && header.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())
        && SecurityContextHolder.getContext().getAuthentication() == null) {
      String token = header.substring(BEARER_PREFIX.length()).trim();
      if (StringUtils.hasText(token)) {
        jwtTokenService.parseToken(token).ifPresent(claims -> tryAuthenticate(claims, request));
      }
    }
    filterChain.doFilter(request, response);
  }

  private void tryAuthenticate(Claims claims, HttpServletRequest request) {
    if (!StringUtils.hasText(claims.getSubject())) {
      return;
    }
    try {
      // 从数据库加载，确保禁用用户与角色变更即时生效
      UserDetails userDetails = userDetailsService.loadUserByUsername(claims.getSubject());
      if (!userDetails.isEnabled() || !userDetails.isAccountNonLocked()) {
        return;
      }
      UsernamePasswordAuthenticationToken authentication =
          new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
      authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
      SecurityContextHolder.getContext().setAuthentication(authentication);
    } catch (AuthenticationException ex) {
      // 含 UsernameNotFoundException（其子类）：保持未认证状态，由授权规则决定 401/403
    }
  }
}

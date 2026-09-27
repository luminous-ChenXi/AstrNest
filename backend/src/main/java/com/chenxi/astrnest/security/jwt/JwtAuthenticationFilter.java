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
 * HTTP Basic 通道不受影响（本过滤器不处理 Basic 头，由 BasicAuthenticationFilter 兜底）。</p>
 *
 * <p><b>令牌滑动刷新</b>：认证成功且剩余有效期不足一半时，签发新 token 放入响应头
 * {@code X-AstrNest-Refreshed-Token}（前端据此滚动更新本地会话）。这就是
 * {@code chenxi.passport.access-token-days} 的「不活动过期」语义：活跃用户持续续期，
 * 完全不活动超过 TTL 才过期。刷新只在认证成功后发生，绝不放宽校验。</p>
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private static final String BEARER_PREFIX = "Bearer ";
  /** 滑动续期响应头：携带新签发的 JWT，前端读到后替换本地 token（CORS exposedHeaders 已放行） */
  public static final String REFRESH_TOKEN_HEADER = "X-AstrNest-Refreshed-Token";

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
        jwtTokenService.parseToken(token).ifPresent(claims -> {
          // 二步验证过渡令牌（purpose=2fa）只能用于 /api/auth/2fa/** 挑战端点，
          // 绝不能建立 API 认证——这里直接拒绝，由授权规则返回 401/403
          if (jwtTokenService.isTwoFactorPendingToken(claims)) {
            return;
          }
          if (tryAuthenticate(claims, request)) {
            slideRefresh(claims, response);
          }
        });
      }
    }
    filterChain.doFilter(request, response);
  }

  private boolean tryAuthenticate(Claims claims, HttpServletRequest request) {
    if (!StringUtils.hasText(claims.getSubject())) {
      return false;
    }
    try {
      // 从数据库加载，确保禁用用户与角色变更即时生效
      UserDetails userDetails = userDetailsService.loadUserByUsername(claims.getSubject());
      if (!userDetails.isEnabled() || !userDetails.isAccountNonLocked()) {
        return false;
      }
      UsernamePasswordAuthenticationToken authentication =
          new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
      authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
      SecurityContextHolder.getContext().setAuthentication(authentication);
      return true;
    } catch (AuthenticationException ex) {
      // 含 UsernameNotFoundException（其子类）：保持未认证状态，由授权规则决定 401/403
      return false;
    }
  }

  /** 认证成功后的滑动续期：剩余有效期不足一半时签发新 token 放入响应头。 */
  private void slideRefresh(Claims claims, HttpServletResponse response) {
    if (!jwtTokenService.needsRefresh(claims)) {
      return;
    }
    try {
      Long userId = claims.get(JwtTokenService.CLAIM_UID, Long.class);
      String username = claims.getSubject();
      if (userId != null && StringUtils.hasText(username)) {
        response.setHeader(REFRESH_TOKEN_HEADER, jwtTokenService.generateToken(userId, username));
      }
    } catch (Exception exception) {
      // 续期失败不影响当次请求：旧 token 在剩余有效期内依然可用
    }
  }
}

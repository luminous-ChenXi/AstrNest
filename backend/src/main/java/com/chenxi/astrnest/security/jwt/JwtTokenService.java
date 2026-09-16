package com.chenxi.astrnest.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * HS256 JWT 签发与校验。
 *
 * <p>密钥来源：{@code astrnest.jwt.secret}（推荐通过环境变量 {@code ASTRNEST_JWT_SECRET} 注入）。
 * 未配置或长度不足 32 字符时，启动阶段生成一次性随机密钥并打 WARN 日志——此时重启后所有已签发 token 失效，
 * 生产环境必须显式配置。
 *
 * <p>claims：{@code sub}=用户名、{@code uid}=用户 id、{@code iat}、{@code exp}。
 */
@Service
public class JwtTokenService {

  private static final Logger log = LoggerFactory.getLogger(JwtTokenService.class);

  static final String CLAIM_UID = "uid";
  private static final int MIN_SECRET_CHARS = 32;
  private static final long DEFAULT_TTL_HOURS = 72L;

  private final SecretKey secretKey;
  private final Duration ttl;

  public JwtTokenService(
      @Value("${astrnest.jwt.secret:}") String configuredSecret,
      @Value("${astrnest.jwt.ttl-hours:72}") long ttlHours) {
    if (StringUtils.hasText(configuredSecret) && configuredSecret.trim().length() >= MIN_SECRET_CHARS) {
      this.secretKey = Keys.hmacShaKeyFor(configuredSecret.trim().getBytes(StandardCharsets.UTF_8));
    } else {
      log.warn("astrnest.jwt.secret 未配置或长度不足 {} 字符，已生成临时随机密钥；"
          + "生产必须配置 ASTRNEST_JWT_SECRET，否则重启后所有 token 失效", MIN_SECRET_CHARS);
      this.secretKey = Jwts.SIG.HS256.key().build();
    }
    long hours = ttlHours > 0 ? ttlHours : DEFAULT_TTL_HOURS;
    this.ttl = Duration.ofHours(hours);
    log.info("JWT 签发器已就绪：TTL={} 小时，密钥来源={}", hours,
        StringUtils.hasText(configuredSecret) ? "配置项 astrnest.jwt.secret" : "临时随机密钥");
  }

  /** 为指定用户签发 JWT（sub=用户名，uid=用户 id，iat/exp 由 TTL 推算）。 */
  public String generateToken(Long userId, String username) {
    Instant now = Instant.now();
    return Jwts.builder()
        .subject(username)
        .claim(CLAIM_UID, userId)
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plus(ttl)))
        .signWith(secretKey)
        .compact();
  }

  /** 校验签名与有效期，返回 claims；无效/过期返回 empty，调用方据此不设置认证。 */
  public Optional<Claims> parseToken(String token) {
    try {
      Claims claims = Jwts.parser()
          .verifyWith(secretKey)
          .build()
          .parseSignedClaims(token)
          .getPayload();
      return Optional.of(claims);
    } catch (JwtException | IllegalArgumentException ex) {
      log.debug("拒绝无效 JWT：{}", ex.getMessage());
      return Optional.empty();
    }
  }

  /** token 有效期（秒），用于登录响应的 expiresIn 字段。 */
  public long ttlSeconds() {
    return ttl.toSeconds();
  }
}

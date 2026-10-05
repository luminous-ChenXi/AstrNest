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
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * HS256 JWT 签发与校验。
 *
 * <p>密钥来源：{@code astrnest.jwt.secret}（推荐通过环境变量 {@code ASTRNEST_JWT_SECRET} 注入）。
 * 未配置或长度不足 32 字符时，启动阶段生成一次性随机密钥并打 WARN 日志——此时重启后所有已签发 token 失效，
 * 生产环境必须显式配置。</p>
 *
 * <p>有效期口径（统一规范）：TTL 取 {@code chenxi.passport.access-token-days}（默认 30 天），
 * 语义为「令牌不活动过期」——{@link JwtAuthenticationFilter} 在剩余有效期不足一半时通过
 * 响应头滑动续期，活跃用户的会话因此持续顺延；超过 TTL 完全不活动才真正过期。</p>
 *
 * <p>claims：{@code sub}=用户名、{@code uid}=用户 id、{@code iat}、{@code exp}。</p>
 */
@Service
public class JwtTokenService {

  private static final Logger log = LoggerFactory.getLogger(JwtTokenService.class);

  static final String CLAIM_UID = "uid";
  /** 令牌用途标记：purpose=2fa 表示「密码已通过、等待二步验证」的过渡令牌，不可用于访问 API */
  public static final String CLAIM_PURPOSE = "purpose";
  public static final String PURPOSE_TWO_FACTOR = "2fa";
  private static final int MIN_SECRET_CHARS = 32;
  private static final long DEFAULT_TTL_DAYS = 30L;
  /** 二步验证过渡令牌有效期：5 分钟内必须完成动态码校验 */
  private static final Duration TWO_FACTOR_PENDING_TTL = Duration.ofMinutes(5);

  private final SecretKey secretKey;
  private final Duration ttl;

  public JwtTokenService(
      @Value("${astrnest.jwt.secret:}") String configuredSecret,
      @Value("${chenxi.passport.access-token-days:30}") long accessTokenDays,
      Environment environment) {
    boolean production = environment.acceptsProfiles(Profiles.of("prod"));
    if (StringUtils.hasText(configuredSecret) && configuredSecret.trim().length() >= MIN_SECRET_CHARS) {
      this.secretKey = Keys.hmacShaKeyFor(configuredSecret.trim().getBytes(StandardCharsets.UTF_8));
    } else if (production) {
      // 与 compose 的数据库密码同等待遇：生产环境密钥缺失直接启动失败，
      // 杜绝"临时随机密钥"静默上线（重启全员掉线 / 多实例密钥不一致）
      throw new IllegalStateException(
          "生产环境必须配置 astrnest.jwt.secret（环境变量 ASTRNEST_JWT_SECRET，至少 " + MIN_SECRET_CHARS
              + " 字符，可用 openssl rand -base64 48 生成）");
    } else {
      log.warn("astrnest.jwt.secret 未配置或长度不足 {} 字符，已生成临时随机密钥；"
          + "生产必须配置 ASTRNEST_JWT_SECRET，否则重启后所有 token 失效", MIN_SECRET_CHARS);
      this.secretKey = Jwts.SIG.HS256.key().build();
    }
    long days = accessTokenDays > 0 ? accessTokenDays : DEFAULT_TTL_DAYS;
    this.ttl = Duration.ofDays(days);
    log.info("JWT 签发器已就绪：TTL={} 天（不活动过期，滑动刷新），密钥来源={}", days,
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

  /**
   * 签发「二步验证过渡令牌」：密码校验通过但尚未通过 TOTP 时发给前端，
   * 仅可用于 /api/auth/2fa/** 完成挑战（服务端校验 purpose），绝不能作为访问令牌
   * （{@link JwtAuthenticationFilter} 对带 purpose 的令牌一律拒绝建立认证）。
   */
  public String generateTwoFactorPendingToken(Long userId, String username) {
    Instant now = Instant.now();
    return Jwts.builder()
        .subject(username)
        .claim(CLAIM_UID, userId)
        .claim(CLAIM_PURPOSE, PURPOSE_TWO_FACTOR)
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plus(TWO_FACTOR_PENDING_TTL)))
        .signWith(secretKey)
        .compact();
  }

  /** 解析二步验证过渡令牌：签名/有效期/purpose 任一不符即返回 empty。 */
  public Optional<Claims> parseTwoFactorPendingToken(String token) {
    return parseToken(token)
        .filter(claims -> PURPOSE_TWO_FACTOR.equals(claims.get(CLAIM_PURPOSE, String.class)));
  }

  /** 是否为二步验证过渡令牌（过滤器据此拒绝其建立 API 认证）。 */
  public boolean isTwoFactorPendingToken(Claims claims) {
    return PURPOSE_TWO_FACTOR.equals(claims.get(CLAIM_PURPOSE, String.class));
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

  /**
   * 是否需要滑动续期：剩余有效期不足 TTL 一半时为 true。
   * 调用方（JwtAuthenticationFilter）据此签发新 token 并放入响应头，实现「不活动过期」语义。
   */
  public boolean needsRefresh(Claims claims) {
    Date expiration = claims.getExpiration();
    if (expiration == null) {
      return false;
    }
    long remainingMillis = expiration.getTime() - System.currentTimeMillis();
    return remainingMillis < ttl.toMillis() / 2;
  }

  /** token 有效期（秒），用于登录响应的 expiresIn 字段。 */
  public long ttlSeconds() {
    return ttl.toSeconds();
  }
}

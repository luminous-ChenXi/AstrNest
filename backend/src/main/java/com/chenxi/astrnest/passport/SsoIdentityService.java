package com.chenxi.astrnest.passport;

import com.chenxi.astrnest.passport.dto.IntrospectionResult;
import com.chenxi.astrnest.passport.dto.PassportToken;
import com.chenxi.astrnest.passport.dto.SsoUserInfo;
import com.chenxi.astrnest.security.dto.UserProfileResponse;
import com.chenxi.astrnest.security.jwt.JwtTokenService;
import com.chenxi.astrnest.security.user.UserAccount;
import com.chenxi.astrnest.security.user.UserAccountRepository;
import com.chenxi.astrnest.security.user.UserAccountService;
import com.chenxi.astrnest.security.user.UserRole;
import com.chenxi.astrnest.security.user.UserRoleRepository;
import com.chenxi.astrnest.user.dto.LoginResponse;
import com.chenxi.astrnest.user.login.UserLoginEventService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/**
 * SSO 影子账号核心编排：凭证自省 → 拉取用户信息 → 影子建档/同步 → 签发本地 JWT。
 *
 * <p><b>影子账号模式</b>：外部身份源（如"辰汐通行证"）首次登录时在 users 表自动创建同源影子账号，
 * 通过 {@code sso_sub}（身份源唯一标识）关联，{@code identity_source} 标记来源。此后每次登录以
 * 身份源 claims 顺带同步昵称/头像/邮箱，本地签发与本地登录完全一致的 JWT，下游权限体系无感知。
 *
 * <p>并发兜底：建档依赖 {@code uk_users_sso_sub} 唯一约束，冲突时回读已有影子账号。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SsoIdentityService {

  /** 本地账号身份来源。 */
  public static final String IDENTITY_SOURCE_LOCAL = "local";

  /**
   * 外部身份源来源标识。取 "passport"（通行证）以贴合辰汐通行证生态的主从站命名；
   * 泛化语义等价于 "sso"——只要非 "local" 即视为外部身份源，后续按 issuer 细分多身份源时
   * 可改造为可配置值（预留扩展点）。
   */
  public static final String IDENTITY_SOURCE_SSO = "passport";

  private static final int USERNAME_MAX_LENGTH = 64;

  private final PassportClient passportClient;
  private final UserAccountRepository userAccountRepository;
  private final UserRoleRepository userRoleRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtTokenService jwtTokenService;
  private final UserAccountService userAccountService;
  private final UserLoginEventService userLoginEventService;

  /**
   * OIDC 授权码 + PKCE 登录：后端凭 code + code_verifier 向通行证换 access_token
   * （public client，无 client_secret），再走统一的影子账号编排。推荐路径，浏览器全程不接触通行证 token。
   *
   * @throws ResponseStatusException 401（授权码无效/已过期、凭证校验未通过）、500（环境问题）
   */
  @Transactional
  public LoginResponse exchangeByAuthorizationCode(String code, String codeVerifier, HttpServletRequest request) {
    PassportToken token = passportClient.exchangeAuthorizationCode(code, codeVerifier);
    return exchangeByAccessToken(token.accessToken(), request);
  }

  /**
   * 凭 access_token 完成登录（遗留路径）：自省校验 → 用户信息 → 影子账号 → 本地 JWT。
   *
   * @throws ResponseStatusException 401（凭证无效/用户信息失败）、500（缺少 USER 角色等环境问题）
   */
  @Transactional
  public LoginResponse exchangeByAccessToken(String accessToken, HttpServletRequest request) {
    IntrospectionResult introspection = passportClient.introspect(accessToken);
    if (!Boolean.TRUE.equals(introspection.active()) || !StringUtils.hasText(introspection.sub())) {
      log.warn("SSO 凭证自省未通过（active=false 或缺失 sub），拒绝登录");
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "SSO 凭证无效");
    }
    SsoUserInfo userInfo = passportClient.fetchUserInfo(accessToken);

    UserAccount user = resolveShadowAccount(introspection.sub().trim(), userInfo);
    syncShadowProfile(user, userInfo);
    userLoginEventService.recordLogin(user, request);

    // 与本地登录一致：装配认证上下文后复用 getCurrentProfile，角色从数据库实时读取
    UsernamePasswordAuthenticationToken authentication =
        new UsernamePasswordAuthenticationToken(user.getUsername(), null, authorities(user));
    SecurityContextHolder.getContext().setAuthentication(authentication);

    String token = jwtTokenService.generateToken(user.getId(), user.getUsername());
    UserProfileResponse profile = userAccountService.getCurrentProfile();
    log.info("SSO 登录成功：userId={}, username={}, source={}", user.getId(), user.getUsername(), user.getIdentitySource());
    return LoginResponse.complete(token, profile, "Bearer", jwtTokenService.ttlSeconds());
  }

  /** 按 sso_sub 查找影子账号；不存在则建档（唯一约束冲突时回读并发兜底）。 */
  private UserAccount resolveShadowAccount(String sub, SsoUserInfo userInfo) {
    Optional<UserAccount> existing = userAccountRepository.findBySsoSub(sub);
    if (existing.isPresent()) {
      return existing.get();
    }
    UserAccount shadow = buildShadowAccount(sub, userInfo);
    try {
      return userAccountRepository.saveAndFlush(shadow);
    } catch (DataIntegrityViolationException ex) {
      // 并发兜底：同 sub 并发首次登录触发 uk_users_sso_sub 冲突时，回读另一请求已建好的影子账号
      log.warn("SSO 影子账号并发建档冲突，回读已有记录：sub 后缀={}", suffix(sub, 6));
      return userAccountRepository.findBySsoSub(sub)
          .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "SSO 登录失败，请稍后重试"));
    }
  }

  private UserAccount buildShadowAccount(String sub, SsoUserInfo userInfo) {
    UserRole userRole = userRoleRepository.findByName("USER")
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "缺少 USER 角色"));
    UserAccount user = new UserAccount();
    user.setSsoSub(sub);
    user.setIdentitySource(IDENTITY_SOURCE_SSO);
    user.setUsername(resolveAvailableUsername(sub, userInfo));
    // 随机 UUID 的 BCrypt：影子账号不存在可用本地密码，杜绝本地口令登录通道
    user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
    user.setDisplayName(resolveDisplayName(userInfo, user.getUsername()));
    user.setEmail(resolveAvailableEmail(sub, userInfo));
    user.setAvatarUrl(clean(userInfo == null ? null : userInfo.avatar()));
    user.setActive(true);
    // 与邮箱注册流程保持一致的普通用户配额
    user.setDailyUploadLimit(50);
    user.setStorageQuotaMb(200L);
    user.getRoles().add(userRole);
    return user;
  }

  /**
   * 每次登录顺带同步影子资料（以主身份源为准）：昵称/头像/邮箱与本地不同则覆盖；
   * 邮箱若已被其他账号占用则跳过，保护本地唯一约束。
   */
  private void syncShadowProfile(UserAccount user, SsoUserInfo userInfo) {
    if (userInfo == null || !IDENTITY_SOURCE_SSO.equals(user.getIdentitySource())) {
      return;
    }
    boolean changed = false;
    String nickname = clean(userInfo.nickname());
    if (StringUtils.hasText(nickname) && !nickname.equals(user.getDisplayName())) {
      user.setDisplayName(nickname);
      changed = true;
    }
    String avatar = clean(userInfo.avatar());
    if (StringUtils.hasText(avatar) && !avatar.equals(user.getAvatarUrl())) {
      user.setAvatarUrl(avatar);
      changed = true;
    }
    String email = clean(userInfo.email());
    if (StringUtils.hasText(email) && !email.equals(user.getEmail())
        && !userAccountRepository.existsByEmail(email)) {
      user.setEmail(email);
      changed = true;
    }
    if (changed) {
      log.info("已同步 SSO 影子账号资料：userId={}", user.getId());
    }
  }

  /** 影子用户名：优先 userinfo.username，冲突时追加 {@code -<sub后6位>}，仍冲突则递增数字后缀。 */
  private String resolveAvailableUsername(String sub, SsoUserInfo userInfo) {
    String base = clean(userInfo == null ? null : userInfo.username());
    if (!StringUtils.hasText(base)) {
      base = "sso_" + sub;
    }
    base = sanitizeUsername(base);
    if (!userAccountRepository.existsByUsername(base)) {
      return base;
    }
    String marker = "-" + suffix(sub, 6);
    String candidate = truncate(base, USERNAME_MAX_LENGTH - marker.length()) + marker;
    int sequence = 1;
    while (userAccountRepository.existsByUsername(candidate)) {
      String tail = marker + "-" + sequence++;
      candidate = truncate(base, USERNAME_MAX_LENGTH - tail.length()) + tail;
    }
    return candidate;
  }

  /** 影子邮箱：userinfo.email 且不与现有账号冲突时采用；否则用 {@code <sub>@sso.local} 占位。 */
  private String resolveAvailableEmail(String sub, SsoUserInfo userInfo) {
    String email = clean(userInfo == null ? null : userInfo.email());
    if (StringUtils.hasText(email) && !userAccountRepository.existsByEmail(email)) {
      return email;
    }
    return sub + "@sso.local";
  }

  private String resolveDisplayName(SsoUserInfo userInfo, String fallback) {
    String nickname = clean(userInfo == null ? null : userInfo.nickname());
    return StringUtils.hasText(nickname) ? nickname : fallback;
  }

  /** 用户名白名单化：去除空白与控制字符，截断到列长，保证与本地用户名同规则存储。 */
  private String sanitizeUsername(String value) {
    String cleaned = value.replaceAll("[\\s\\p{Cntrl}]+", "");
    return truncate(cleaned, USERNAME_MAX_LENGTH);
  }

  private String truncate(String value, int maxLength) {
    if (maxLength <= 0) {
      return "";
    }
    return value.length() <= maxLength ? value : value.substring(0, maxLength);
  }

  private String suffix(String value, int length) {
    if (!StringUtils.hasText(value)) {
      return "000000";
    }
    return value.length() <= length ? value : value.substring(value.length() - length);
  }

  private String clean(String value) {
    return StringUtils.hasText(value) ? value.trim() : null;
  }

  private List<GrantedAuthority> authorities(UserAccount user) {
    return user.getRoles().stream()
        .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
        .collect(Collectors.toList());
  }
}

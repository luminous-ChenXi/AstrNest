package com.chenxi.astrnest.user;

import com.chenxi.astrnest.security.bruteforce.AuthProtectionService;
import com.chenxi.astrnest.security.dto.UserProfileResponse;
import com.chenxi.astrnest.security.jwt.JwtTokenService;
import com.chenxi.astrnest.security.totp.TotpAccountService;
import com.chenxi.astrnest.security.user.UserAccount;
import com.chenxi.astrnest.security.user.UserAccountRepository;
import com.chenxi.astrnest.security.user.UserAccountService;
import com.chenxi.astrnest.system.SystemConfigService;
import com.chenxi.astrnest.user.dto.LoginRequest;
import com.chenxi.astrnest.user.dto.LoginResponse;
import com.chenxi.astrnest.user.dto.TwoFactorSetupConfirmResponse;
import com.chenxi.astrnest.user.dto.TwoFactorVerifyRequest;
import com.chenxi.astrnest.user.login.UserLoginEventService;
import com.chenxi.astrnest.common.ClientIpResolver;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * 用户名/密码登录编排：防爆破校验 → DaoAuthenticationProvider 认证 → （站长开关开启时的）
 * TOTP 二步验证分流 → JWT 签发 → 登录事件记录 → profile 组装。
 *
 * <p>二步验证分流（login.totp_required=true 时）：</p>
 * <ul>
 *   <li>未绑定用户：返回 TOTP_SETUP（otpauth URI + 密钥，密钥以未确认绑定落库），
 *       前端扫码确认后才发正式 JWT；</li>
 *   <li>已绑定用户：返回 TOTP_CHALLENGE（过渡令牌），输码换发正式 JWT——过渡令牌带
 *       purpose=2fa 标记，JWT 过滤器拒绝其建立 API 认证，只能走 2FA 挑战端点。</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

  private final AuthenticationManager authenticationManager;
  private final JwtTokenService jwtTokenService;
  private final UserAccountRepository userAccountRepository;
  private final UserAccountService userAccountService;
  private final UserLoginEventService userLoginEventService;
  private final AuthProtectionService authProtectionService;
  private final ClientIpResolver clientIpResolver;
  private final SystemConfigService systemConfigService;
  private final TotpAccountService totpAccountService;

  @Transactional
  public LoginResponse login(LoginRequest request, HttpServletRequest httpRequest) {
    String ip = clientIpResolver.resolve(httpRequest);
    authProtectionService.ensureLoginAllowed(request.username(), ip);
    try {
      Authentication authentication = authenticationManager.authenticate(
          new UsernamePasswordAuthenticationToken(request.username(), request.password())
      );
      SecurityContextHolder.getContext().setAuthentication(authentication);
      UserAccount user = resolveUserAccount(authentication.getName());
      authProtectionService.recordLoginSuccess(request.username(), ip);
      userLoginEventService.recordLogin(user, httpRequest);

      // 站长开启登录二步验证：密码通过后进入挑战/强制绑定，不发正式 JWT
      if (systemConfigService.isTotpRequired()) {
        return twoFactorResponse(user);
      }

      return issueCompleteResponse(user);
    } catch (AuthenticationException ex) {
      authProtectionService.recordLoginFailure(request.username(), ip);
      log.warn("登录认证失败：user={}, type={}", request.username(), ex.getClass().getSimpleName());
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户名或密码错误", ex);
    }
  }

  /** 二步验证挑战：输 6 位动态码（或 8 位还原码）换发正式 JWT。 */
  @Transactional
  public LoginResponse verifyTwoFactor(TwoFactorVerifyRequest request, HttpServletRequest httpRequest) {
    UserAccount user = resolvePendingUser(request.tempToken());
    String ip = clientIpResolver.resolve(httpRequest);
    try {
      totpAccountService.verifyChallenge(user.getId(), request.code());
    } catch (ResponseStatusException exception) {
      authProtectionService.recordLoginFailure(user.getUsername(), ip);
      throw exception;
    }
    authProtectionService.recordLoginSuccess(user.getUsername(), ip);
    return issueCompleteResponse(user);
  }

  /**
   * 强制绑定确认：校验 6 位动态码 → 绑定落库（confirmed）→ 一次性发放还原码 → 直接完成登录。
   */
  @Transactional
  public TwoFactorSetupConfirmResponse confirmTwoFactorSetup(TwoFactorVerifyRequest request) {
    UserAccount user = resolvePendingUser(request.tempToken());
    List<String> recoveryCodes = totpAccountService.confirmSetup(user.getId(), request.code());
    LoginResponse complete = issueCompleteResponse(user);
    return new TwoFactorSetupConfirmResponse(
        complete.token(), complete.profile(), complete.tokenType(), complete.expiresIn(), recoveryCodes);
  }

  // ==================== 内部装配 ====================

  /** 按开关状态构造二步验证响应：未绑定 → 强制绑定（SETUP）；已绑定 → 挑战（CHALLENGE）。 */
  private LoginResponse twoFactorResponse(UserAccount user) {
    String tempToken = jwtTokenService.generateTwoFactorPendingToken(user.getId(), user.getUsername());
    if (!totpAccountService.isBound(user.getId())) {
      TotpAccountService.TotpSetup setup = totpAccountService.startSetup(user.getId(), user.getUsername());
      return LoginResponse.totpSetup(tempToken, setup.otpauthUri(), setup.secret(), user.getUsername());
    }
    return LoginResponse.totpChallenge(tempToken, user.getUsername());
  }

  private LoginResponse issueCompleteResponse(UserAccount user) {
    // 二步验证换发端点（/api/auth/2fa/**）不携带认证态（匿名令牌不算）：
    // 组装 profile 前先把当前用户放入 SecurityContext（与 login 端点同口径，角色仍由数据库实时读取）
    Authentication existing = SecurityContextHolder.getContext().getAuthentication();
    boolean anonymous = existing == null
        || !existing.isAuthenticated()
        || existing instanceof org.springframework.security.authentication.AnonymousAuthenticationToken;
    if (anonymous) {
      SecurityContextHolder.getContext().setAuthentication(
          new UsernamePasswordAuthenticationToken(user.getUsername(), null, List.of()));
    }
    String token = jwtTokenService.generateToken(user.getId(), user.getUsername(), user.getTokenVersion());
    // 复用既有装配逻辑：profile 角色从数据库实时读取（SecurityContext 已在登录时设置）
    UserProfileResponse profile = userAccountService.getCurrentProfile();
    return LoginResponse.complete(token, profile, "Bearer", jwtTokenService.ttlSeconds());
  }

  /** 校验过渡令牌并解析出用户（仅接受 purpose=2fa 的短期令牌）。 */
  private UserAccount resolvePendingUser(String tempToken) {
    Claims claims = jwtTokenService.parseTwoFactorPendingToken(tempToken)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
            "二步验证会话已过期，请重新登录"));
    return userAccountRepository.findByUsername(claims.getSubject())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户不存在，请重新登录"));
  }

  // ==================== 工具 ====================

  private UserAccount resolveUserAccount(String principal) {
    String normalized = principal == null ? "" : principal.trim();
    if (normalized.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户名或密码错误");
    }
    return userAccountRepository.findByUsername(normalized)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户名或密码错误"));
  }
}

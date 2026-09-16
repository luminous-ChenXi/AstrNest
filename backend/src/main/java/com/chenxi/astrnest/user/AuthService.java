package com.chenxi.astrnest.user;

import com.chenxi.astrnest.security.bruteforce.AuthProtectionService;
import com.chenxi.astrnest.security.dto.UserProfileResponse;
import com.chenxi.astrnest.security.jwt.JwtTokenService;
import com.chenxi.astrnest.security.user.UserAccount;
import com.chenxi.astrnest.security.user.UserAccountRepository;
import com.chenxi.astrnest.security.user.UserAccountService;
import com.chenxi.astrnest.user.dto.LoginRequest;
import com.chenxi.astrnest.user.dto.LoginResponse;
import com.chenxi.astrnest.user.login.UserLoginEventService;
import com.chenxi.astrnest.common.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
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
 * 用户名/密码登录编排：防爆破校验 → DaoAuthenticationProvider 认证 → JWT 签发 → 登录事件记录 → profile 组装。
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
      String token = jwtTokenService.generateToken(user.getId(), user.getUsername());
      // 复用既有装配逻辑，角色从数据库实时读取
      UserProfileResponse profile = userAccountService.getCurrentProfile();
      return new LoginResponse(token, profile, "Bearer", jwtTokenService.ttlSeconds());
    } catch (AuthenticationException ex) {
      authProtectionService.recordLoginFailure(request.username(), ip);
      log.warn("登录认证失败：user={}, type={}", request.username(), ex.getClass().getSimpleName());
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户名或密码错误", ex);
    }
  }

  private UserAccount resolveUserAccount(String principal) {
    String normalized = principal == null ? "" : principal.trim();
    if (normalized.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户名或密码错误");
    }
    return userAccountRepository.findByUsername(normalized)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户名或密码错误"));
  }
}

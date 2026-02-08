package com.chenxi.astrnest.user;

import com.chenxi.astrnest.security.bruteforce.AuthProtectionService;
import com.chenxi.astrnest.security.dto.UserProfileResponse;
import com.chenxi.astrnest.security.user.UserAccount;
import com.chenxi.astrnest.security.user.UserAccountRepository;
import com.chenxi.astrnest.security.user.UserRole;
import com.chenxi.astrnest.user.dto.LoginRequest;
import com.chenxi.astrnest.user.dto.LoginResponse;
import com.chenxi.astrnest.user.login.UserLoginEventService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthenticationManager authenticationManager;
  private final UserAccountRepository userAccountRepository;
  private final UserLoginEventService userLoginEventService;
  private final AuthProtectionService authProtectionService;

  @PostMapping("/login")
  @Transactional
  public LoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
    String ip = resolveClientIp(httpRequest);
    authProtectionService.ensureLoginAllowed(request.username(), ip);
    try {
      Authentication authentication = authenticationManager.authenticate(
          new UsernamePasswordAuthenticationToken(request.username(), request.password())
      );
      SecurityContextHolder.getContext().setAuthentication(authentication);
      UserAccount user = resolveUserAccount(request.username());
      authProtectionService.recordLoginSuccess(request.username(), ip);
      String token = Base64.getEncoder()
          .encodeToString((request.username() + ":" + request.password()).getBytes(StandardCharsets.UTF_8));
      Set<String> roles = user.getRoles().stream().map(UserRole::getName).collect(Collectors.toSet());
      userLoginEventService.recordLogin(user, httpRequest);
      UserProfileResponse profile = new UserProfileResponse(
          user.getId(),
          user.getUsername(),
          user.getDisplayName(),
          user.getEmail(),
          user.getAvatarUrl(),
          user.getWebsite(),
          user.getSignature(),
          user.getLocation(),
          user.getLoginIpHistory(),
          user.getLastLoginIp(),
          user.getLastLoginAt(),
          roles
      );
      return new LoginResponse("Basic " + token, profile);
    } catch (AuthenticationException ex) {
      authProtectionService.recordLoginFailure(request.username(), ip);
      // 添加调试日志
      System.err.println("[DEBUG] 认证失败: " + ex.getClass().getName() + ": " + ex.getMessage());
      ex.printStackTrace();
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户名或密码错误", ex);
    }
  }

  private UserAccount resolveUserAccount(String principal) {
    String normalized = principal == null ? "" : principal.trim();
    if (normalized.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户名或密码错误");
    }
    return userAccountRepository.findByUsername(normalized)
        .or(() -> userAccountRepository.findByEmail(normalized.toLowerCase(Locale.ROOT)))
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户名或密码错误"));
  }

  private String resolveClientIp(HttpServletRequest request) {
    if (request == null) return "unknown";
    String[] headers = {"X-Forwarded-For", "X-Real-IP", "CF-Connecting-IP"};
    for (String header : headers) {
      String value = request.getHeader(header);
      if (value != null && !value.isBlank()) {
        return value.split(",")[0].trim();
      }
    }
    return request.getRemoteAddr();
  }
}

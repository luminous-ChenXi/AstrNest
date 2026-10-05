package com.chenxi.astrnest.security.auth;

import com.chenxi.astrnest.security.user.UserAccount;
import java.util.Collection;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;

/**
 * 认证主体：在 Spring Security {@link User} 语义之上携带 {@code tokenVersion}，
 * 供 {@code JwtAuthenticationFilter} 比对 JWT 的 ver claim——版本不一致即拒绝，
 * 实现「改密/找回密码后吊销全部旧令牌」。
 */
@Getter
public class ChenxiUserDetails extends org.springframework.security.core.userdetails.User {

  private final long tokenVersion;

  public ChenxiUserDetails(UserAccount user, Collection<? extends GrantedAuthority> authorities) {
    super(user.getUsername(), user.getPassword(),
        user.isActive(),   // enabled
        true,              // accountNonExpired
        true,              // credentialsNonExpired
        user.isActive(),   // accountNonLocked
        authorities);
    this.tokenVersion = user.getTokenVersion();
  }
}

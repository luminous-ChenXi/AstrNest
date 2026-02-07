package com.imgbed.security.auth;

import com.imgbed.security.user.UserAccount;
import com.imgbed.security.user.UserAccountRepository;
import java.util.Collection;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class DatabaseUserDetailsService implements UserDetailsService {

  private final UserAccountRepository userAccountRepository;

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    UserAccount user = findByPrincipal(username)
        .orElseThrow(() -> new UsernameNotFoundException("用户不存在"));
    Collection<GrantedAuthority> authorities = user.getRoles().stream()
        .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
        .collect(Collectors.toSet());
    return org.springframework.security.core.userdetails.User
        .withUsername(user.getUsername())
        .password(user.getPassword())
        .authorities(authorities)
        .accountLocked(!user.isActive())
        .disabled(!user.isActive())
        .build();
  }

  private Optional<UserAccount> findByPrincipal(String principal) {
    if (!StringUtils.hasText(principal)) {
      return Optional.empty();
    }
    String trimmed = principal.trim();
    Optional<UserAccount> byUsername = userAccountRepository.findByUsername(trimmed);
    if (byUsername.isPresent()) {
      return byUsername;
    }
    String normalizedEmail = trimmed.toLowerCase(Locale.ROOT);
    return userAccountRepository.findByEmail(normalizedEmail);
  }
}

package com.imgbed.security.apikey.auth;

import com.imgbed.security.apikey.ApiKey;
import java.util.Collections;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public class ApiKeyAuthenticationToken extends AbstractAuthenticationToken {

  private final String principal;
  private final ApiKey apiKey;

  public ApiKeyAuthenticationToken(String principal, ApiKey apiKey) {
    super(Collections.singletonList(new SimpleGrantedAuthority("ROLE_API_CLIENT")));
    this.principal = principal;
    this.apiKey = apiKey;
    setAuthenticated(true);
  }

  @Override
  public Object getCredentials() {
    return "";
  }

  @Override
  public Object getPrincipal() {
    return principal;
  }

  public ApiKey getApiKey() {
    return apiKey;
  }
}

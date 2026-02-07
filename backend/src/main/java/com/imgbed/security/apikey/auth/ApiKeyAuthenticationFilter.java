package com.imgbed.security.apikey.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.imgbed.common.ApiErrorResponse;
import com.imgbed.security.apikey.ApiKey;
import com.imgbed.security.apikey.ApiKeyProperties;
import com.imgbed.security.apikey.ApiKeyService;
import com.imgbed.security.apikey.exception.ApiKeyAuthenticationException;
import com.imgbed.security.apikey.exception.ApiKeyQuotaExceededException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

  private final ApiKeyService apiKeyService;
  private final ApiKeyProperties properties;
  private final ObjectMapper objectMapper;

  public ApiKeyAuthenticationFilter(ApiKeyService apiKeyService, ApiKeyProperties properties, ObjectMapper objectMapper) {
    this.apiKeyService = apiKeyService;
    this.properties = properties;
    this.objectMapper = objectMapper;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String headerValue = request.getHeader(properties.getHeaderName());
    if (!StringUtils.hasText(headerValue) || SecurityContextHolder.getContext().getAuthentication() != null) {
      filterChain.doFilter(request, response);
      return;
    }
    try {
      ApiKey apiKey = apiKeyService.authenticate(headerValue);
      ApiKeyAuthenticationToken authentication = new ApiKeyAuthenticationToken(apiKey.getName(), apiKey);
      authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
      SecurityContextHolder.getContext().setAuthentication(authentication);
      filterChain.doFilter(request, response);
    } catch (ApiKeyAuthenticationException ex) {
      writeError(response, HttpStatus.UNAUTHORIZED, ex.getMessage());
    } catch (ApiKeyQuotaExceededException ex) {
      writeError(response, HttpStatus.TOO_MANY_REQUESTS, ex.getMessage());
    }
  }

  private void writeError(HttpServletResponse response, HttpStatus status, String message) throws IOException {
    if (response.isCommitted()) {
      return;
    }
    response.setStatus(status.value());
    response.setContentType("application/json");
    ApiErrorResponse body = new ApiErrorResponse(message, status.value(), Instant.now());
    objectMapper.writeValue(response.getWriter(), body);
  }
}

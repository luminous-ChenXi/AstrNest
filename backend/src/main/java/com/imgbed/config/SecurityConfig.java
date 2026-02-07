package com.imgbed.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.imgbed.security.apikey.ApiKeyProperties;
import com.imgbed.security.apikey.ApiKeyService;
import com.imgbed.security.apikey.auth.ApiKeyAuthenticationFilter;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

  private final UserDetailsService userDetailsService;

  public SecurityConfig(UserDetailsService userDetailsService) {
    this.userDetailsService = userDetailsService;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http, ApiKeyAuthenticationFilter apiKeyAuthenticationFilter) throws Exception {
    http
        .csrf(csrf -> csrf.disable())
        .cors(Customizer.withDefaults())
        .userDetailsService(userDetailsService)
        .addFilterBefore(apiKeyAuthenticationFilter, BasicAuthenticationFilter.class)
        .authorizeHttpRequests(authorize -> authorize
            .requestMatchers(
                "/actuator/health",
                "/actuator/info",
                "/swagger-ui/**",
                "/v3/api-docs/**"
            ).permitAll()
            .requestMatchers("/api/auth/login").permitAll()
            .requestMatchers("/api/auth/chenxi/**").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/monitor/**").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/gallery/**").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/gallery/**").permitAll()
            .requestMatchers(HttpMethod.DELETE, "/api/gallery/**").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/uploads/**").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/public/users/**").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/upload/limits").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/system/public-config").permitAll()
            .requestMatchers(new AntPathRequestMatcher("/upload/**", "GET"), new AntPathRequestMatcher("/upload/**", "HEAD")).permitAll()
            .requestMatchers(HttpMethod.POST, "/api/uploads/**").hasAnyRole("ADMIN", "API_CLIENT", "USER")
            .requestMatchers("/api/admin/**").hasRole("ADMIN")
            .requestMatchers("/api/user/**").hasAnyRole("ADMIN", "USER")
            .anyRequest().authenticated()
        )
        .httpBasic(Customizer.withDefaults());

    return http.build();
  }

  @Bean
  public ApiKeyAuthenticationFilter apiKeyAuthenticationFilter(ApiKeyService apiKeyService, ApiKeyProperties apiKeyProperties, ObjectMapper objectMapper) {
    return new ApiKeyAuthenticationFilter(apiKeyService, apiKeyProperties, objectMapper);
  }

  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
    return configuration.getAuthenticationManager();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOriginPatterns(List.of(
        "http://localhost:*",
        "http://127.0.0.1:*",
        "http://192.168.*.*:*",
        "*"
    ));
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(List.of("*"));
    configuration.setExposedHeaders(List.of("Content-Disposition", "X-RateLimit-Remaining", "X-RateLimit-Reset"));
    configuration.setAllowCredentials(true);
    configuration.setMaxAge(3600L);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  @Bean
  public WebSecurityCustomizer webSecurityCustomizer() {
    return web -> web.ignoring().requestMatchers("/upload/**");
  }
}

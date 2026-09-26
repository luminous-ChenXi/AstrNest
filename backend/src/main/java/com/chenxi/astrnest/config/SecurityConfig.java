package com.chenxi.astrnest.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.chenxi.astrnest.security.apikey.ApiKeyProperties;
import com.chenxi.astrnest.security.apikey.ApiKeyService;
import com.chenxi.astrnest.security.apikey.auth.ApiKeyAuthenticationFilter;
import com.chenxi.astrnest.security.jwt.JwtAuthenticationFilter;
import com.chenxi.astrnest.security.jwt.JwtTokenService;
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
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
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
  private final CorsProperties corsProperties;

  public SecurityConfig(UserDetailsService userDetailsService, CorsProperties corsProperties) {
    this.userDetailsService = userDetailsService;
    this.corsProperties = corsProperties;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http,
      ApiKeyAuthenticationFilter apiKeyAuthenticationFilter,
      JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
    http
        .csrf(csrf -> csrf.disable())
        .cors(Customizer.withDefaults())
        .userDetailsService(userDetailsService)
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
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
            // 安装向导：匿名可探测状态/执行安装。安全性由 InstallGuardFilter（未安装时拦截其余 /api/**）
            // 与 InstallController（防重装锁命中后写端点一律 403）双重守卫，这里仅负责放行。
            .requestMatchers("/api/install/**").permitAll()
            // 通行证登录公开接口（chenxi.passport.*，默认关闭）：GET config 供前端拉取身份源配置、
            // POST exchange 以 OIDC 授权码+PKCE（或遗留 access_token）换取本地 JWT。
            // 功能默认关闭：config 返回 enabled=false，exchange 返回明确错误。
            .requestMatchers("/api/auth/sso/**").permitAll()
            // 授权状态公开只读端点（chenxi.license.*，默认关闭）：前端据此决定是否展示授权横幅。
            .requestMatchers("/api/license/status").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/monitor/**").hasRole("ADMIN")
            .requestMatchers(HttpMethod.GET, "/api/gallery/**").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/gallery/**").permitAll()
            .requestMatchers(HttpMethod.DELETE, "/api/gallery/**").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/uploads/**").permitAll()
            .requestMatchers(HttpMethod.GET, "/embed/**").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/public/users/**").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/public/announcements/**").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/upload/limits").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/system/public-config").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/albums/public/**").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/albums/featured").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/albums/random/**").permitAll()
            .requestMatchers(HttpMethod.GET, "/album/**").permitAll()
            .requestMatchers(HttpMethod.GET, "/picture/**").permitAll()
            .requestMatchers(new AntPathRequestMatcher("/api/public/assets/**", "GET"), new AntPathRequestMatcher("/api/public/assets/**", "HEAD")).permitAll()
            .requestMatchers(new AntPathRequestMatcher("/upload/**", "GET"), new AntPathRequestMatcher("/upload/**", "HEAD")).permitAll()
            .requestMatchers(HttpMethod.POST, "/api/uploads/**").hasAnyRole("ADMIN", "API_CLIENT", "USER")
            .requestMatchers("/api/admin/**").hasRole("ADMIN")
            .requestMatchers("/api/user/**").hasAnyRole("ADMIN", "USER")
            .anyRequest().authenticated()
        )
        .httpBasic(Customizer.withDefaults())
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.disable()));

    return http.build();
  }

  @Bean
  public ApiKeyAuthenticationFilter apiKeyAuthenticationFilter(ApiKeyService apiKeyService, ApiKeyProperties apiKeyProperties, ObjectMapper objectMapper) {
    return new ApiKeyAuthenticationFilter(apiKeyService, apiKeyProperties, objectMapper);
  }

  @Bean
  public JwtAuthenticationFilter jwtAuthenticationFilter(JwtTokenService jwtTokenService, UserDetailsService userDetailsService) {
    return new JwtAuthenticationFilter(jwtTokenService, userDetailsService);
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
    configuration.setAllowedOrigins(corsProperties.getAllowedOrigins());
    configuration.setAllowedMethods(corsProperties.getAllowedMethods());
    configuration.setAllowedHeaders(corsProperties.getAllowedHeaders());
    configuration.setExposedHeaders(corsProperties.getExposedHeaders());
    configuration.setAllowCredentials(corsProperties.isAllowCredentials());
    configuration.setMaxAge(corsProperties.getMaxAge());
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  @Bean
  public WebSecurityCustomizer webSecurityCustomizer() {
    return web -> web.ignoring().requestMatchers("/upload/**");
  }
}

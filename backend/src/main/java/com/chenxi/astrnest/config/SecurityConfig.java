package com.chenxi.astrnest.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.chenxi.astrnest.security.apikey.ApiKeyProperties;
import com.chenxi.astrnest.security.apikey.ApiKeyService;
import com.chenxi.astrnest.security.apikey.auth.ApiKeyAuthenticationFilter;
import com.chenxi.astrnest.security.jwt.JwtAuthenticationFilter;
import com.chenxi.astrnest.security.jwt.JwtTokenService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
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
  private final Environment environment;

  public SecurityConfig(UserDetailsService userDetailsService, CorsProperties corsProperties,
      Environment environment) {
    this.userDetailsService = userDetailsService;
    this.corsProperties = corsProperties;
    this.environment = environment;
  }

  /** Swagger 是否匿名放行：仅 dev profile 放行，生产环境一律要求认证（双保险之一，另见 application-prod.yml 关闭 springdoc）。 */
  private boolean swaggerPermitAll() {
    return environment.acceptsProfiles(Profiles.of("dev"));
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
        .authorizeHttpRequests(authorize -> {
            authorize.requestMatchers(
                "/actuator/health",
                "/actuator/info"
            ).permitAll();
            if (swaggerPermitAll()) {
                authorize.requestMatchers(
                    "/swagger-ui/**",
                    "/v3/api-docs/**"
                ).permitAll();
            }
            authorize.requestMatchers("/api/auth/login").permitAll()
            // TOTP 二步验证挑战/绑定确认：凭登录时下发的 5 分钟过渡令牌调用（purpose=2fa，
            // 该令牌被 JwtAuthenticationFilter 拒绝建立 API 认证，仅可走这两个端点）
            .requestMatchers("/api/auth/2fa/**").permitAll()
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
            .requestMatchers("/api/user/**").hasAnyRole("ADMIN", "USER");
            authorize.anyRequest().authenticated();
        })
        // httpBasic 已移除：Basic 通道的认证失败不经过 AuthProtectionService 防爆破锁定，
        // 等于给暴力破解留了无限速旁路；机器对机器场景由 API Key（X-API-Key）承担
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        // frameOptions 保持 disable：/embed/** 面向站外 iframe 分享（前端提供「嵌入代码」复制），
        // 不能写 SAMEORIGIN/DENY。全局 X-Frame-Options:SAMEORIGIN 由 GlobalSecurityHeaderFilter
        // 统一下发并对 /embed/** 豁免，避免此处 HeaderWriterFilter 覆盖其取值。
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

  /**
   * BCrypt 默认强度由 10 提升至 12（对齐 LuomiBlog 基线）：
   * 仅影响新密码 / 新 TOTP 还原码的哈希成本；BCrypt 哈希自带强度前缀（$2a$10 / $2a$12），
   * 校验时按哈希自身的强度自动识别，存量 $2a$10 密码验证不受影响。
   */
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
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

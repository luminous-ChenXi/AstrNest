package com.chenxi.astrnest.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.chenxi.astrnest.security.apikey.ApiKeyProperties;
import com.chenxi.astrnest.security.apikey.ApiKeyService;
import com.chenxi.astrnest.security.apikey.auth.ApiKeyAuthenticationFilter;
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

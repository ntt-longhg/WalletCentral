package com.gateway.walletcentral.config;

import com.gateway.walletcentral.modules.auth.service.AuthService;
import com.gateway.walletcentral.modules.tenant.model.Tenant;
import com.gateway.walletcentral.modules.tenant.repository.TenantRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String API_KEY_HEADER = "X-API-Key";
    public static final String REQUEST_ATTR_EMAIL = "authenticatedEmail";

    private final TenantRepository tenantRepository;
    private final AuthService authService;

    public SecurityConfig(TenantRepository tenantRepository, AuthService authService) {
        this.tenantRepository = tenantRepository;
        this.authService = authService;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/ws/**").permitAll()
                        .anyRequest().permitAll())
                .addFilterBefore(apiKeyFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public OncePerRequestFilter apiKeyFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request,
                    HttpServletResponse response,
                    FilterChain filterChain) throws ServletException, IOException {
                String path = request.getRequestURI();

                // Skip auth for swagger, api-docs, actuator
                if (path.startsWith("/swagger-ui") || path.startsWith("/v3/api-docs")
                        || path.startsWith("/actuator")) {
                    filterChain.doFilter(request, response);
                    return;
                }

                // Skip auth for WebSocket (SockJS info, transport, websocket upgrade)
                if (path.startsWith("/ws")) {
                    filterChain.doFilter(request, response);
                    return;
                }

                // Skip auth for public OTP endpoints (send + verify only)
                if (path.startsWith("/api/v1/auth/otp/")) {
                    filterChain.doFilter(request, response);
                    return;
                }

                // Skip auth for public password-login endpoints (mode-gated in AuthService)
                if (path.equals("/api/v1/auth/login-config")
                        || path.equals("/api/v1/auth/password/login")
                        || path.equals("/api/v1/auth/password/setup")) {
                    filterChain.doFilter(request, response);
                    return;
                }

                String apiKey = request.getHeader(API_KEY_HEADER);
                if (apiKey == null || apiKey.isBlank()) {
                    response.setStatus(HttpStatus.UNAUTHORIZED.value());
                    response.setContentType("application/json");
                    response.getWriter().write("{\"success\":false,\"message\":\"Invalid or missing API key\"}");
                    return;
                }

                // 1. Check admin session token (UUID format from OTP login)
                if (authService.isValidAdminToken(apiKey)) {
                    // Resolve email from token and set as request attribute
                    String email = authService.getEmailFromToken(apiKey);
                    if (email != null) {
                        request.setAttribute(REQUEST_ATTR_EMAIL, email);
                    }
                    filterChain.doFilter(request, response);
                    return;
                }

                // 2. Check tenant API key format: "client_id:client_secret" (EMBED - unchanged)
                if (apiKey.contains(":")) {
                    String[] parts = apiKey.split(":", 2);
                    String clientId = parts[0];
                    String clientSecret = parts[1];

                    Optional<Tenant> tenantOpt = tenantRepository.findByClientId(clientId);
                    if (tenantOpt.isPresent()) {
                        Tenant tenant = tenantOpt.get();
                        // Validate client_secret matches
                        if (tenant.getClientSecret().equals(clientSecret)
                                && tenant
                                        .getStatus() == com.gateway.walletcentral.modules.tenant.model.TenantStatus.ACTIVE) {
                            filterChain.doFilter(request, response);
                            return;
                        }
                    }
                }

                // 3. Invalid API key
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.setContentType("application/json");
                response.getWriter().write("{\"success\":false,\"message\":\"Invalid or expired API key\"}");
            }
        };
    }
}

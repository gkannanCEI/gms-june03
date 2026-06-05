package com.gms.config;

import com.gms.security.LocalUserDetailsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${gms.cors.allowed-origins:http://localhost:42001}")
    private String allowedOrigins;

    @Value("${gms.security.auth-mode:b2c}")
    private String authMode;

    /**
     * Security filter chain for Azure AD B2C JWT authentication (production mode).
     * Active when gms.security.auth-mode=b2c (default).
     */
    @Bean
    @ConditionalOnProperty(name = "gms.security.auth-mode", havingValue = "b2c", matchIfMissing = true)
    public SecurityFilterChain b2cFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/favicon.ico", "/static/**", "/error", "/error.html").permitAll()
                .requestMatchers("/api/internal/**").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/api/me").authenticated()
                .requestMatchers("/api/admin/**", "/maintenance/**", "/admin-dashboard.html")
                    .hasAuthority("ROLE_ADMIN")
                .requestMatchers("/api/programs/**", "/api/applications/**")
                    .hasAnyAuthority("ROLE_APPLICANT", "ROLE_ADMIN")
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(new GmsJwtAuthenticationConverter()))
            );
        return http.build();
    }

    /**
     * Security filter chain for local username/password authentication (development/testing).
     * Active when gms.security.auth-mode=local.
     * Uses HTTP Basic auth against the gms_user table with BCrypt passwords.
     */
    @Bean
    @ConditionalOnProperty(name = "gms.security.auth-mode", havingValue = "local")
    public SecurityFilterChain localFilterChain(HttpSecurity http,
                                                LocalUserDetailsService userDetailsService) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/favicon.ico", "/static/**", "/error", "/error.html").permitAll()
                .requestMatchers("/api/internal/**").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/api/me").authenticated()
                .requestMatchers("/api/admin/**", "/maintenance/**", "/admin-dashboard.html")
                    .hasAuthority("ROLE_ADMIN")
                .requestMatchers("/api/programs/**", "/api/applications/**")
                    .hasAnyAuthority("ROLE_APPLICANT", "ROLE_ADMIN")
                .anyRequest().authenticated()
            )
            .httpBasic(basic -> basic.authenticationEntryPoint(suppressWwwAuthenticateEntryPoint()))
            .authenticationProvider(localAuthProvider(userDetailsService));
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @ConditionalOnProperty(name = "gms.security.auth-mode", havingValue = "local")
    public DaoAuthenticationProvider localAuthProvider(LocalUserDetailsService userDetailsService) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * Custom entry point that returns a plain 401 JSON response without a
     * WWW-Authenticate: Basic header.  Without this, browsers intercept the 401
     * and display their own login dialog instead of letting Angular handle it.
     */
    @Bean
    @ConditionalOnProperty(name = "gms.security.auth-mode", havingValue = "local")
    public BasicAuthenticationEntryPoint suppressWwwAuthenticateEntryPoint() {
        BasicAuthenticationEntryPoint ep = new BasicAuthenticationEntryPoint() {
            @Override
            public void commence(jakarta.servlet.http.HttpServletRequest request,
                                 jakarta.servlet.http.HttpServletResponse response,
                                 org.springframework.security.core.AuthenticationException authException)
                    throws java.io.IOException {
                // Return a clean 401 without WWW-Authenticate so the browser
                // passes the response back to Angular rather than showing its own dialog.
                response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Unauthorized\"}");
            }
        };
        ep.setRealmName("GMS");
        return ep;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}

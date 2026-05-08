package com.terry.duey.auth;

import com.terry.duey.config.DueyProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        return http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(
                        auth ->
                                auth.requestMatchers("/api/auth/google")
                                        .permitAll()
                                        .anyRequest()
                                        .authenticated())
                .addFilterBefore(
                        jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    JwtAuthenticationFilter jwtAuthenticationFilter(
            JwtService jwtService, Environment environment, DueyProperties dueyProperties) {
        boolean debugProfile = Arrays.asList(environment.getActiveProfiles()).contains("debug");
        return new JwtAuthenticationFilter(
                jwtService, debugProfile, dueyProperties.auth().debugUserId());
    }

    static class JwtAuthenticationFilter extends OncePerRequestFilter {
        private final JwtService jwtService;
        private final boolean debugAuthEnabled;
        private final String debugUserId;

        JwtAuthenticationFilter(
                JwtService jwtService, boolean debugAuthEnabled, String debugUserId) {
            this.jwtService = jwtService;
            this.debugAuthEnabled = debugAuthEnabled;
            this.debugUserId = debugUserId;
        }

        @Override
        protected void doFilterInternal(
                HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                throws ServletException, IOException {
            String header = request.getHeader("Authorization");
            if (header != null && header.startsWith("Bearer ")) {
                jwtService
                        .parseUserId(header.substring("Bearer ".length()))
                        .ifPresent(
                                userId ->
                                        SecurityContextHolder.getContext()
                                                .setAuthentication(
                                                        new UsernamePasswordAuthenticationToken(
                                                                new UserPrincipal(userId),
                                                                null,
                                                                null)));
            } else if (debugAuthEnabled && debugUserId != null && !debugUserId.isBlank()) {
                SecurityContextHolder.getContext()
                        .setAuthentication(
                                new UsernamePasswordAuthenticationToken(
                                        new UserPrincipal(debugUserId), null, null));
            }
            filterChain.doFilter(request, response);
        }
    }
}

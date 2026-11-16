package com.taskflow.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authenticates the request from an {@code Authorization: Bearer <access token>} header.
 *
 * <p>This filter never rejects a request itself. With a missing or invalid token it simply
 * leaves the security context empty; the authorization rules in {@link SecurityConfig} then
 * decide whether the route needs authentication and answer 401 through the entry point.
 *
 * <p>It is deliberately not a {@code @Component}: Spring Boot would also register it as a
 * plain servlet filter for every request, outside the security filter chain.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length()).trim();
            jwtService.parseAccessToken(token).ifPresent(user -> authenticate(user, request));
        }

        filterChain.doFilter(request, response);
    }

    private void authenticate(AuthenticatedUser user, HttpServletRequest request) {
        // Three-argument constructor = already authenticated. No credentials are kept and there
        // are no global authorities: roles in TaskFlow are per workspace, checked in services.
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(user, null, List.of());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        // Always create a fresh context instead of mutating the current one (thread safety).
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
    }
}

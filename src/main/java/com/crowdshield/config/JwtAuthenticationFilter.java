package com.crowdshield.config;

import com.crowdshield.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    // Filters incoming requests, validates JWT tokens, and sets authentication context for admin endpoints
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        // Skip JWT check for public endpoints
        if (path.startsWith("/api/admin/auth/login") ||
            path.startsWith("/api/content") ||
            path.startsWith("/actuator") ||
            path.startsWith("/api/info") ||
            path.startsWith("/api/rules") ||
            path.equals("/") ||
            path.equals("/index.html") ||
            path.equals("/admin") ||
            path.equals("/admin.html") ||
            path.startsWith("/static/")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Check for JWT token in Authorization header
        String authHeader = request.getHeader("Authorization");
        String token = null;
        String username = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
            try {
                username = jwtUtil.extractUsername(token);
            } catch (Exception e) {
                log.error("Error extracting username from token", e);
            }
        }

        // Validate token
        if (token != null && username != null && jwtUtil.validateToken(token)) {
            // Set authentication in security context
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            username,
                            null,
                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))
                    );
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } else if (path.startsWith("/api/admin")) {
            // Admin endpoints require authentication
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"Invalid or missing token\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }
}


package com.techstore.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/** Adds browser hardening headers to API responses. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class SecurityHeadersFilter extends OncePerRequestFilter {

    private static final String CONTENT_SECURITY_POLICY =
            "default-src 'none'; frame-ancestors 'none'; base-uri 'none'";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (isApiRequest(request)) {
            response.setHeader("X-Content-Type-Options", "nosniff");
            response.setHeader("X-Frame-Options", "DENY");
            response.setHeader("Referrer-Policy", "no-referrer");
            response.setHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=()");
            response.setHeader("Content-Security-Policy", CONTENT_SECURITY_POLICY);
            response.setHeader("Cache-Control", "no-store");
        }
        filterChain.doFilter(request, response);
    }

    private boolean isApiRequest(HttpServletRequest request) {
        String contextPath = request.getContextPath();
        return request.getRequestURI().startsWith(contextPath + "/api/");
    }
}

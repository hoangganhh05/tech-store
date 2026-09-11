package com.techstore.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techstore.dto.response.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Small in-process login throttle. It limits each source address and login
 * endpoint independently, and only records failed responses.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class LoginRateLimitFilter extends OncePerRequestFilter {

    static final int MAX_FAILED_ATTEMPTS = 5;
    static final Duration WINDOW = Duration.ofMinutes(1);
    private static final String RATE_LIMIT_CODE = "RATE_LIMIT_EXCEEDED";
    private static final String RATE_LIMIT_MESSAGE = "Quá nhiều lần đăng nhập thất bại. Vui lòng thử lại sau.";

    private final ObjectMapper objectMapper;
    private final Map<String, AttemptWindow> attempts = new ConcurrentHashMap<>();

    public LoginRateLimitFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (!isLoginRequest(request) || "OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = request.getRemoteAddr() + "|" + request.getRequestURI();
        Instant now = Instant.now();
        long retryAfter = retryAfterSeconds(key, now);
        if (retryAfter > 0) {
            writeRateLimitResponse(response, retryAfter);
            return;
        }

        try {
            filterChain.doFilter(request, response);
        } catch (ServletException | IOException | RuntimeException exception) {
            recordFailure(key, now);
            throw exception;
        } finally {
            int status = response.getStatus();
            if (status >= 400 && status != HttpStatus.TOO_MANY_REQUESTS.value()) {
                recordFailure(key, Instant.now());
            } else if (status >= 200 && status < 400) {
                attempts.remove(key);
            }
        }
    }

    private boolean isLoginRequest(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.endsWith("/auth/login") || path.endsWith("/auth/admin/login");
    }

    private long retryAfterSeconds(String key, Instant now) {
        AttemptWindow window = attempts.get(key);
        if (window == null) {
            return 0;
        }
        synchronized (window) {
            if (window.isExpired(now)) {
                attempts.remove(key, window);
                return 0;
            }
            if (window.failedAttempts < MAX_FAILED_ATTEMPTS) {
                return 0;
            }
            return Math.max(1, Duration.between(now, window.startedAt.plus(WINDOW)).toSeconds());
        }
    }

    private void recordFailure(String key, Instant now) {
        AttemptWindow window = attempts.computeIfAbsent(key, ignored -> new AttemptWindow(now));
        synchronized (window) {
            if (window.isExpired(now)) {
                window.startedAt = now;
                window.failedAttempts = 0;
            }
            window.failedAttempts++;
        }
    }

    private void writeRateLimitResponse(HttpServletResponse response, long retryAfter) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Retry-After", Long.toString(retryAfter));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), ApiResponse.error(RATE_LIMIT_CODE, RATE_LIMIT_MESSAGE));
    }

    private static final class AttemptWindow {
        private Instant startedAt;
        private int failedAttempts;

        private AttemptWindow(Instant startedAt) {
            this.startedAt = startedAt;
        }

        private boolean isExpired(Instant now) {
            return !now.isBefore(startedAt.plus(WINDOW));
        }
    }
}

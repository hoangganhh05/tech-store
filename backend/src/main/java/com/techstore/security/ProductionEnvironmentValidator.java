package com.techstore.security;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Arrays;

/**
 * Reject deployment settings that are safe only during local development.
 * Secrets themselves are supplied by the deployment environment, never source control.
 */
@Component
@Profile("prod")
public class ProductionEnvironmentValidator {

    private static final String DEVELOPMENT_JWT_SECRET_MARKER = "techstore-development-jwt-secret";

    private final JwtProperties jwtProperties;
    private final String allowedOrigins;
    private final String passwordResetFrontendUrl;

    public ProductionEnvironmentValidator(
            JwtProperties jwtProperties,
            @Value("${app.cors.allowed-origins}") String allowedOrigins,
            @Value("${app.password-reset.frontend-base-url}") String passwordResetFrontendUrl
    ) {
        this.jwtProperties = jwtProperties;
        this.allowedOrigins = allowedOrigins;
        this.passwordResetFrontendUrl = passwordResetFrontendUrl;
    }

    @PostConstruct
    void validate() {
        String jwtSecret = jwtProperties.getSecret();
        if (jwtSecret != null && jwtSecret.contains(DEVELOPMENT_JWT_SECRET_MARKER)) {
            throw new IllegalStateException("JWT_SECRET for prod must not reuse the development secret");
        }

        Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .forEach(this::validateTrustedUrl);
        validateTrustedUrl(passwordResetFrontendUrl);
    }

    private void validateTrustedUrl(String value) {
        if (value == null || value.isBlank() || "*".equals(value.trim())) {
            throw new IllegalStateException("Production URLs must be explicit and must not use a wildcard");
        }

        URI uri;
        try {
            uri = URI.create(value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("Production URL is invalid", exception);
        }

        boolean localHttp = "http".equalsIgnoreCase(uri.getScheme())
                && "localhost".equalsIgnoreCase(uri.getHost());
        if ((!"https".equalsIgnoreCase(uri.getScheme()) && !localHttp) || uri.getHost() == null) {
            throw new IllegalStateException("Production URLs must use HTTPS (localhost is allowed for container smoke tests)");
        }
    }
}

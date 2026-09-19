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
    private static final String LEGACY_BOOTSTRAP_EMAIL = "admin@techstore.local";
    private static final String LEGACY_BOOTSTRAP_PASSWORD = "Admin@123456";

    private final JwtProperties jwtProperties;
    private final InitialAdminProperties initialAdminProperties;
    private final String allowedOrigins;
    private final String passwordResetFrontendUrl;

    public ProductionEnvironmentValidator(
            JwtProperties jwtProperties,
            InitialAdminProperties initialAdminProperties,
            @Value("${app.cors.allowed-origins}") String allowedOrigins,
            @Value("${app.password-reset.frontend-base-url}") String passwordResetFrontendUrl
    ) {
        this.jwtProperties = jwtProperties;
        this.initialAdminProperties = initialAdminProperties;
        this.allowedOrigins = allowedOrigins;
        this.passwordResetFrontendUrl = passwordResetFrontendUrl;
    }

    @PostConstruct
    void validate() {
        String jwtSecret = jwtProperties.getSecret();
        if (jwtSecret != null && jwtSecret.contains(DEVELOPMENT_JWT_SECRET_MARKER)) {
            throw new IllegalStateException("JWT_SECRET for prod must not reuse the development secret");
        }

        validateInitialAdmin();

        Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .forEach(this::validateTrustedUrl);
        validateTrustedUrl(passwordResetFrontendUrl);
    }

    private void validateInitialAdmin() {
        String email = initialAdminProperties.getEmail() == null ? "" : initialAdminProperties.getEmail().trim();
        String password = initialAdminProperties.getPassword();
        String fullName = initialAdminProperties.getFullName() == null ? "" : initialAdminProperties.getFullName().trim();

        if (email.isBlank() || password == null || password.isBlank()) {
            throw new IllegalStateException("INITIAL_ADMIN_EMAIL and INITIAL_ADMIN_PASSWORD are required for prod");
        }
        if (LEGACY_BOOTSTRAP_EMAIL.equalsIgnoreCase(email) || LEGACY_BOOTSTRAP_PASSWORD.equals(password)) {
            throw new IllegalStateException("Production must not use the legacy default administrator credentials");
        }
        if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$") || email.length() > 255) {
            throw new IllegalStateException("INITIAL_ADMIN_EMAIL is invalid");
        }
        if (password.length() < 12 || password.length() > 72) {
            throw new IllegalStateException("INITIAL_ADMIN_PASSWORD must contain 12 to 72 characters");
        }
        if (fullName.isBlank() || fullName.length() > 150) {
            throw new IllegalStateException("INITIAL_ADMIN_FULL_NAME is invalid");
        }
        if (initialAdminProperties.getPhone() != null && initialAdminProperties.getPhone().trim().length() > 20) {
            throw new IllegalStateException("INITIAL_ADMIN_PHONE is invalid");
        }
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

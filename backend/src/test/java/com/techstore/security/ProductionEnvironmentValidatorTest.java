package com.techstore.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProductionEnvironmentValidatorTest {

    @Test
    void rejectsTheDevelopmentJwtSecretInProduction() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("techstore-development-jwt-secret-change-before-production-2026");

        ProductionEnvironmentValidator validator = new ProductionEnvironmentValidator(
                properties,
                validInitialAdmin(),
                "https://store.example.com",
                "https://store.example.com"
        );

        assertThrows(IllegalStateException.class, validator::validate);
    }

    @Test
    void acceptsADistinctSecretAndExplicitTrustedUrls() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("production-only-secret-that-is-longer-than-thirty-two-characters");

        ProductionEnvironmentValidator validator = new ProductionEnvironmentValidator(
                properties,
                validInitialAdmin(),
                "https://store.example.com,https://admin.example.com",
                "https://store.example.com"
        );

        assertDoesNotThrow(validator::validate);
    }

    @Test
    void rejectsWildcardAndNonHttpsProductionUrls() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("production-only-secret-that-is-longer-than-thirty-two-characters");

        assertThrows(IllegalStateException.class, () -> new ProductionEnvironmentValidator(
                properties,
                validInitialAdmin(),
                "*",
                "https://store.example.com"
        ).validate());

        assertThrows(IllegalStateException.class, () -> new ProductionEnvironmentValidator(
                properties,
                validInitialAdmin(),
                "https://store.example.com",
                "http://store.example.com"
        ).validate());
    }

    @Test
    void rejectsMissingOrLegacyInitialAdminCredentials() {
        JwtProperties properties = productionJwtProperties();
        InitialAdminProperties missing = new InitialAdminProperties();

        assertThrows(IllegalStateException.class, () -> new ProductionEnvironmentValidator(
                properties,
                missing,
                "https://store.example.com",
                "https://store.example.com"
        ).validate());

        InitialAdminProperties legacy = validInitialAdmin();
        legacy.setEmail("admin@techstore.local");
        assertThrows(IllegalStateException.class, () -> new ProductionEnvironmentValidator(
                properties,
                legacy,
                "https://store.example.com",
                "https://store.example.com"
        ).validate());
    }

    private static JwtProperties productionJwtProperties() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("production-only-secret-that-is-longer-than-thirty-two-characters");
        return properties;
    }

    private static InitialAdminProperties validInitialAdmin() {
        InitialAdminProperties properties = new InitialAdminProperties();
        properties.setEmail("operations-admin@example.com");
        properties.setPassword("initial-admin-password-2026");
        properties.setFullName("Operations Administrator");
        return properties;
    }
}

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
                "*",
                "https://store.example.com"
        ).validate());

        assertThrows(IllegalStateException.class, () -> new ProductionEnvironmentValidator(
                properties,
                "https://store.example.com",
                "http://store.example.com"
        ).validate());
    }
}

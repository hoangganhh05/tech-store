package com.techstore.security;

import com.techstore.entity.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class JwtTokenIssuer implements TokenIssuer {

    private static final int MINIMUM_HMAC_SECRET_LENGTH = 32;

    private final JwtProperties properties;
    private SecretKey signingKey;

    public JwtTokenIssuer(JwtProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void initialize() {
        String secret = properties.getSecret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT_SECRET must be configured");
        }
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < MINIMUM_HMAC_SECRET_LENGTH) {
            throw new IllegalStateException("JWT_SECRET must contain at least 32 characters");
        }
        if (properties.getAccessTokenTtl().isNegative() || properties.getAccessTokenTtl().isZero()
                || properties.getRefreshTokenTtl().isNegative() || properties.getRefreshTokenTtl().isZero()) {
            throw new IllegalStateException("JWT token TTL values must be greater than zero");
        }
        signingKey = Keys.hmacShaKeyFor(secretBytes);
    }

    @Override
    public IssuedTokenPair issue(User user) {
        Instant issuedAt = Instant.now();
        Instant accessExpiresAt = issuedAt.plus(properties.getAccessTokenTtl());
        Instant refreshExpiresAt = issuedAt.plus(properties.getRefreshTokenTtl());
        List<String> roles = user.getRoleCodes().stream().map(Enum::name).sorted().toList();

        return new IssuedTokenPair(
                createToken(user, roles, "access", issuedAt, accessExpiresAt),
                createToken(user, roles, "refresh", issuedAt, refreshExpiresAt),
                accessExpiresAt,
                refreshExpiresAt
        );
    }

    private String createToken(
            User user,
            List<String> roles,
            String tokenType,
            Instant issuedAt,
            Instant expiresAt
    ) {
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("uid", user.getId())
                .claim("roles", roles)
                .claim("type", tokenType)
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey)
                .compact();
    }
}

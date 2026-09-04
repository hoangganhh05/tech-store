package com.techstore.security;

import java.time.Instant;

public record IssuedTokenPair(
        String accessToken,
        String refreshToken,
        Instant accessTokenExpiresAt,
        Instant refreshTokenExpiresAt
) {
}

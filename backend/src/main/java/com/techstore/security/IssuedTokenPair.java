package com.techstore.security;

import java.time.Instant;

public record IssuedTokenPair(
        String accessToken,
        String refreshToken,
        String refreshTokenId,
        Instant accessTokenExpiresAt,
        Instant refreshTokenExpiresAt
) {
}

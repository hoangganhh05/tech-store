package com.techstore.dto.response;

import java.time.Instant;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        Instant accessTokenExpiresAt,
        Instant refreshTokenExpiresAt,
        UserResponse user
) {
}

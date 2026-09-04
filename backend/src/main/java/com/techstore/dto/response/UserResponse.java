package com.techstore.dto.response;

import java.time.Instant;
import java.util.Set;

public record UserResponse(
        Long id,
        String email,
        String fullName,
        String phone,
        String status,
        Set<String> roles,
        boolean emailVerified,
        Instant createdAt
) {
}

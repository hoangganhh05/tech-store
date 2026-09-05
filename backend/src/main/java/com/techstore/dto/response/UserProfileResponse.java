package com.techstore.dto.response;

import java.time.Instant;
import java.time.LocalDate;

public record UserProfileResponse(
        Long id,
        String email,
        String fullName,
        String phone,
        LocalDate dateOfBirth,
        Instant updatedAt
) {
}

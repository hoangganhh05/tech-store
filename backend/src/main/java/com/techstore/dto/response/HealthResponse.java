package com.techstore.dto.response;

import java.time.Instant;

public record HealthResponse(
        String application,
        String status,
        String database,
        Instant checkedAt
) {
}

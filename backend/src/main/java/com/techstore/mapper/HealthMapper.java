package com.techstore.mapper;

import com.techstore.dto.response.HealthResponse;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class HealthMapper {

    public HealthResponse toResponse(String databaseStatus) {
        return new HealthResponse("techstore-backend", "UP", databaseStatus, Instant.now());
    }
}

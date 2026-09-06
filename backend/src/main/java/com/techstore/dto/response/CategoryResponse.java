package com.techstore.dto.response;

import java.time.Instant;

public record CategoryResponse(
        Long id,
        String name,
        String description,
        Long parentId,
        String parentName,
        String imageUrl,
        Integer displayOrder,
        Boolean isActive,
        Instant createdAt,
        Instant updatedAt
) {
}
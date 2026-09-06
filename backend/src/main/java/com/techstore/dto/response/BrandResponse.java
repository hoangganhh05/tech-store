package com.techstore.dto.response;

import com.techstore.entity.Brand;

import java.time.Instant;

public record BrandResponse(
        Long id,
        String name,
        String logoUrl,
        String description,
        Instant createdAt,
        Instant updatedAt
) {
    public static BrandResponse from(Brand brand) {
        return new BrandResponse(
                brand.getId(),
                brand.getName(),
                brand.getLogoUrl(),
                brand.getDescription(),
                brand.getCreatedAt(),
                brand.getUpdatedAt()
        );
    }
}

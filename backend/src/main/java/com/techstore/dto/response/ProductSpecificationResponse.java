package com.techstore.dto.response;

import com.techstore.entity.ProductSpecification;

import java.time.Instant;

public record ProductSpecificationResponse(
        Long id,
        Long productId,
        String specKey,
        String specValue,
        Integer displayOrder,
        Instant createdAt,
        Instant updatedAt
) {
    public static ProductSpecificationResponse from(ProductSpecification spec) {
        return new ProductSpecificationResponse(
                spec.getId(),
                spec.getProduct().getId(),
                spec.getSpecKey(),
                spec.getSpecValue(),
                spec.getDisplayOrder(),
                spec.getCreatedAt(),
                spec.getUpdatedAt()
        );
    }
}

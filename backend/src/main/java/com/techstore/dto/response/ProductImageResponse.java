package com.techstore.dto.response;

import com.techstore.entity.ProductImage;

import java.time.Instant;

public record ProductImageResponse(
        Long id,
        Long productId,
        Long variantId,
        String variantSku,
        String variantColor,
        String imageUrl,
        Boolean isPrimary,
        Integer displayOrder,
        Instant createdAt,
        Instant updatedAt
) {
    public static ProductImageResponse from(ProductImage image) {
        return new ProductImageResponse(
                image.getId(),
                image.getProduct().getId(),
                image.getVariant() != null ? image.getVariant().getId() : null,
                image.getVariant() != null ? image.getVariant().getSku() : null,
                image.getVariant() != null ? image.getVariant().getColor() : null,
                image.getImageUrl(),
                image.getIsPrimary(),
                image.getDisplayOrder(),
                image.getCreatedAt(),
                image.getUpdatedAt()
        );
    }
}

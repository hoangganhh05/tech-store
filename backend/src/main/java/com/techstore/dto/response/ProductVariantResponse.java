package com.techstore.dto.response;

import com.techstore.entity.ProductVariant;
import com.techstore.enums.VariantStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record ProductVariantResponse(
        Long id,
        Long productId,
        String productName,
        String sku,
        String color,
        String storage,
        BigDecimal price,
        BigDecimal originalPrice,
        Integer stockQuantity,
        VariantStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static ProductVariantResponse from(ProductVariant variant) {
        return new ProductVariantResponse(
                variant.getId(),
                variant.getProduct().getId(),
                variant.getProduct().getName(),
                variant.getSku(),
                variant.getColor(),
                variant.getStorage(),
                variant.getPrice(),
                variant.getOriginalPrice(),
                variant.getStockQuantity(),
                variant.getStatus(),
                variant.getCreatedAt(),
                variant.getUpdatedAt()
        );
    }
}

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
        String stockStatus,
        Instant createdAt,
        Instant updatedAt
) {
    public static ProductVariantResponse from(ProductVariant variant) {
        return from(variant, variant.getStockQuantity());
    }

    public static ProductVariantResponse from(ProductVariant variant, Integer stockQuantity) {
        int qty = stockQuantity != null ? Math.max(0, stockQuantity) : 0;
        String stockStatus;
        if (qty <= 0) {
            stockStatus = "OUT_OF_STOCK";
        } else if (qty <= 5) {
            stockStatus = "LOW_STOCK";
        } else {
            stockStatus = "IN_STOCK";
        }

        return new ProductVariantResponse(
                variant.getId(),
                variant.getProduct().getId(),
                variant.getProduct().getName(),
                variant.getSku(),
                variant.getColor(),
                variant.getStorage(),
                variant.getPrice(),
                variant.getOriginalPrice(),
                qty,
                variant.getStatus(),
                stockStatus,
                variant.getCreatedAt(),
                variant.getUpdatedAt()
        );
    }

    public static ProductVariantResponse from(ProductVariant variant, BigDecimal price, BigDecimal originalPrice) {
        return from(variant, price, originalPrice, variant.getStockQuantity());
    }

    public static ProductVariantResponse from(
            ProductVariant variant,
            BigDecimal price,
            BigDecimal originalPrice,
            Integer stockQuantity
    ) {
        ProductVariantResponse base = from(variant, stockQuantity);
        return new ProductVariantResponse(base.id(), base.productId(), base.productName(), base.sku(), base.color(),
                base.storage(), price, originalPrice, base.stockQuantity(), base.status(), base.stockStatus(),
                base.createdAt(), base.updatedAt());
    }
}

package com.techstore.dto.response;

import com.techstore.entity.Product;
import com.techstore.enums.ProductStatus;

import java.time.Instant;

public record ProductResponse(
        Long id,
        String name,
        String description,
        Long brandId,
        String brandName,
        Long categoryId,
        String categoryName,
        ProductStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getBrand().getId(),
                product.getBrand().getName(),
                product.getCategory().getId(),
                product.getCategory().getName(),
                product.getStatus(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}


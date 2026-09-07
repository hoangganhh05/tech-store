package com.techstore.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

public record StorefrontProductResponse(
        Long id,
        String name,
        String description,
        Long brandId,
        String brandName,
        Long categoryId,
        String categoryName,
        String thumbnailUrl,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        BigDecimal originalPrice,
        Integer discountPercent,
        Integer totalStock,
        Boolean hasStock,
        Long salesCount,
        Double rating,
        Instant createdAt
) {
}


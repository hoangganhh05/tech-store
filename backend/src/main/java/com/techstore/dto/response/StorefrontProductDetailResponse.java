package com.techstore.dto.response;

import com.techstore.enums.ProductStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record StorefrontProductDetailResponse(
        Long id,
        String name,
        String description,
        Long brandId,
        String brandName,
        Long categoryId,
        String categoryName,
        ProductStatus status,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        BigDecimal originalPrice,
        Integer discountPercent,
        Integer totalStock,
        Boolean hasStock,
        Long salesCount,
        Double rating,
        List<ProductVariantResponse> variants,
        List<ProductImageResponse> images,
        List<ProductSpecificationResponse> specifications,
        Instant createdAt,
        Instant updatedAt
) {
}

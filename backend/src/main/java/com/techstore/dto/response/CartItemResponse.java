package com.techstore.dto.response;

import java.math.BigDecimal;

public record CartItemResponse(
        Long id,
        Long variantId,
        Long productId,
        String productName,
        String sku,
        String color,
        String storage,
        BigDecimal price,
        BigDecimal originalPrice,
        String imageUrl,
        Integer quantity,
        Integer availableStock,
        BigDecimal subtotal
) {
}


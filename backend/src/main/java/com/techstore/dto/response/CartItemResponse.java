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
        BigDecimal subtotal,
        boolean hasStockIssue,
        String stockStatusMessage
) {
    public CartItemResponse(
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
        this(id, variantId, productId, productName, sku, color, storage, price, originalPrice, imageUrl, quantity, availableStock, subtotal, false, null);
    }
}

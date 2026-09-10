package com.techstore.dto.response;

public record LowStockReportItem(
        Long variantId,
        Long productId,
        String productName,
        Long categoryId,
        String categoryName,
        String sku,
        String color,
        String storage,
        Integer quantityOnHand,
        Integer quantityReserved,
        Integer availableQuantity,
        Integer lowStockThreshold,
        String stockStatus
) {
}

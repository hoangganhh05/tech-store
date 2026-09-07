package com.techstore.dto.response;

public record VariantStockResponse(
        Long variantId,
        Long productId,
        String sku,
        Integer stockQuantity,
        String stockStatus,
        Boolean isAvailable
) {
    public static VariantStockResponse of(Long variantId, Long productId, String sku, Integer stockQuantity) {
        int qty = stockQuantity != null ? Math.max(0, stockQuantity) : 0;
        String status;
        if (qty <= 0) {
            status = "OUT_OF_STOCK";
        } else if (qty <= 5) {
            status = "LOW_STOCK";
        } else {
            status = "IN_STOCK";
        }
        boolean available = qty > 0;
        return new VariantStockResponse(variantId, productId, sku, qty, status, available);
    }
}

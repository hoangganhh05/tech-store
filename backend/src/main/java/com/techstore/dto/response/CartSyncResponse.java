package com.techstore.dto.response;

public record CartSyncResponse(
        CartResponse cart,
        int mergedItemsCount,
        boolean hasStockAdjusted,
        String message
) {
}

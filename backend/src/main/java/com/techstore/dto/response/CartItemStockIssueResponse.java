package com.techstore.dto.response;

import com.techstore.enums.StockIssueType;

public record CartItemStockIssueResponse(
        Long itemId,
        Long variantId,
        String productName,
        String sku,
        int requestedQuantity,
        int availableStock,
        StockIssueType issueType,
        String message
) {
}


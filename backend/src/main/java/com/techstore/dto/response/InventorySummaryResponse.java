package com.techstore.dto.response;

public record InventorySummaryResponse(
        long totalVariants,
        long inStockCount,
        long lowStockCount,
        long outOfStockCount
) {}

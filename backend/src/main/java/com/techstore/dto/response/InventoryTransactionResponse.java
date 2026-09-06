package com.techstore.dto.response;

import com.techstore.enums.InventoryTransactionType;

import java.time.Instant;

public record InventoryTransactionResponse(
        Long id,
        Long inventoryId,
        Long variantId,
        String productName,
        String sku,
        String color,
        String storage,
        InventoryTransactionType transactionType,
        Integer quantityChange,
        String referenceType,
        Long referenceId,
        String note,
        Long createdById,
        String createdByName,
        String createdByEmail,
        Instant createdAt
) {
}
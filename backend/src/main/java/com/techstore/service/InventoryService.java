package com.techstore.service;

import com.techstore.dto.request.InventoryImportRequest;
import com.techstore.dto.response.InventoryResponse;
import com.techstore.dto.response.InventorySummaryResponse;
import com.techstore.dto.response.InventoryTransactionResponse;
import com.techstore.dto.response.PageResponse;
import com.techstore.entity.ProductVariant;
import com.techstore.enums.InventoryTransactionType;
import com.techstore.enums.StockStatus;
import org.springframework.data.domain.Pageable;

public interface InventoryService {

    PageResponse<InventoryResponse> getInventories(String search, Long categoryId, StockStatus stockStatus, Pageable pageable);

    InventorySummaryResponse getInventorySummary();

    InventoryResponse getInventoryByVariantId(Long variantId);

    InventoryResponse importInventory(Long currentUserId, InventoryImportRequest request);

    PageResponse<InventoryTransactionResponse> getTransactions(Long variantId, InventoryTransactionType type, Pageable pageable);

    void ensureInventoryForVariant(ProductVariant variant, int initialStock);
}

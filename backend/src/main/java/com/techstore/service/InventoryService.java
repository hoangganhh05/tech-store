package com.techstore.service;

import com.techstore.dto.request.InventoryAdjustmentRequest;
import com.techstore.dto.request.InventoryImportRequest;
import com.techstore.dto.request.OrderInventoryDeductionRequest;
import com.techstore.dto.request.OrderInventoryRestoreRequest;
import com.techstore.dto.request.UpdateThresholdRequest;
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

    InventoryResponse adjustInventory(Long currentUserId, InventoryAdjustmentRequest request);

    void deductInventoryForOrder(Long currentUserId, OrderInventoryDeductionRequest request);

    void restoreInventoryForOrder(Long currentUserId, OrderInventoryRestoreRequest request);

    PageResponse<InventoryResponse> getLowStockInventories(String search, Long categoryId, Pageable pageable);

    InventoryResponse updateLowStockThreshold(Long variantId, UpdateThresholdRequest request);

    PageResponse<InventoryTransactionResponse> getTransactions(Long variantId, InventoryTransactionType type, Pageable pageable);

    void ensureInventoryForVariant(ProductVariant variant, int initialStock);
}

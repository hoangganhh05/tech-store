package com.techstore.service;

import com.techstore.dto.response.InventoryResponse;
import com.techstore.dto.response.InventorySummaryResponse;
import com.techstore.dto.response.PageResponse;
import com.techstore.entity.ProductVariant;
import com.techstore.enums.StockStatus;
import org.springframework.data.domain.Pageable;

public interface InventoryService {

    PageResponse<InventoryResponse> getInventories(String search, Long categoryId, StockStatus stockStatus, Pageable pageable);

    InventorySummaryResponse getInventorySummary();

    InventoryResponse getInventoryByVariantId(Long variantId);

    void ensureInventoryForVariant(ProductVariant variant, int initialStock);
}

package com.techstore.service.impl;

import com.techstore.dto.response.InventoryResponse;
import com.techstore.dto.response.InventorySummaryResponse;
import com.techstore.dto.response.PageResponse;
import com.techstore.entity.Inventory;
import com.techstore.entity.ProductVariant;
import com.techstore.enums.ErrorCode;
import com.techstore.enums.StockStatus;
import com.techstore.exception.BusinessException;
import com.techstore.repository.InventoryRepository;
import com.techstore.service.InventoryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;

    public InventoryServiceImpl(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<InventoryResponse> getInventories(String search, Long categoryId, StockStatus stockStatus, Pageable pageable) {
        String trimmedSearch = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        String statusFilter = (stockStatus != null) ? stockStatus.name() : "ALL";

        Page<Inventory> page = inventoryRepository.findWithFilters(trimmedSearch, categoryId, statusFilter, pageable);
        return PageResponse.of(page.map(InventoryResponse::from));
    }

    @Override
    @Transactional(readOnly = true)
    public InventorySummaryResponse getInventorySummary() {
        long totalVariants = inventoryRepository.countActiveVariants();
        long inStockCount = inventoryRepository.countInStock();
        long lowStockCount = inventoryRepository.countLowStock();
        long outOfStockCount = inventoryRepository.countOutOfStock();

        return new InventorySummaryResponse(totalVariants, inStockCount, lowStockCount, outOfStockCount);
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryResponse getInventoryByVariantId(Long variantId) {
        Inventory inventory = inventoryRepository.findByVariantId(variantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVENTORY_NOT_FOUND, "Không tìm thấy thông tin tồn kho cho biến thể ID: " + variantId));
        return InventoryResponse.from(inventory);
    }

    @Override
    @Transactional
    public void ensureInventoryForVariant(ProductVariant variant, int initialStock) {
        if (inventoryRepository.findByVariantId(variant.getId()).isEmpty()) {
            Inventory inventory = new Inventory(variant, Math.max(0, initialStock), 0, 5);
            inventoryRepository.save(inventory);
        }
    }
}

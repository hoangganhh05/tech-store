package com.techstore.dto.response;

import com.techstore.entity.Inventory;
import com.techstore.entity.Product;
import com.techstore.entity.ProductVariant;

import java.math.BigDecimal;
import java.time.Instant;

public record InventoryResponse(
        Long id,
        Long variantId,
        String sku,
        Long productId,
        String productName,
        String categoryName,
        String brandName,
        String color,
        String storage,
        BigDecimal price,
        Integer quantityOnHand,
        Integer quantityReserved,
        Integer availableQuantity,
        Integer lowStockThreshold,
        String stockStatus,
        Instant updatedAt
) {
    public static InventoryResponse from(Inventory inventory) {
        ProductVariant variant = inventory.getVariant();
        Product product = variant != null ? variant.getProduct() : null;

        return new InventoryResponse(
                inventory.getId(),
                variant != null ? variant.getId() : null,
                variant != null ? variant.getSku() : null,
                product != null ? product.getId() : null,
                product != null ? product.getName() : null,
                product != null && product.getCategory() != null ? product.getCategory().getName() : null,
                product != null && product.getBrand() != null ? product.getBrand().getName() : null,
                variant != null ? variant.getColor() : null,
                variant != null ? variant.getStorage() : null,
                variant != null ? variant.getPrice() : null,
                inventory.getQuantityOnHand(),
                inventory.getQuantityReserved(),
                inventory.getAvailableQuantity(),
                inventory.getLowStockThreshold(),
                inventory.getStockStatus().name(),
                inventory.getUpdatedAt()
        );
    }
}

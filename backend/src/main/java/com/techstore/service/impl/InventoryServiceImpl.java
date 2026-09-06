package com.techstore.service.impl;

import com.techstore.dto.request.InventoryAdjustmentRequest;
import com.techstore.dto.request.InventoryImportRequest;
import com.techstore.dto.response.InventoryResponse;
import com.techstore.dto.response.InventorySummaryResponse;
import com.techstore.dto.response.InventoryTransactionResponse;
import com.techstore.dto.response.PageResponse;
import com.techstore.entity.Inventory;
import com.techstore.entity.InventoryTransaction;
import com.techstore.entity.ProductVariant;
import com.techstore.entity.User;
import com.techstore.enums.ErrorCode;
import com.techstore.enums.InventoryTransactionType;
import com.techstore.enums.StockStatus;
import com.techstore.exception.BusinessException;
import com.techstore.repository.InventoryRepository;
import com.techstore.repository.InventoryTransactionRepository;
import com.techstore.repository.ProductVariantRepository;
import com.techstore.repository.UserRepository;
import com.techstore.service.InventoryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final ProductVariantRepository productVariantRepository;
    private final UserRepository userRepository;

    public InventoryServiceImpl(
            InventoryRepository inventoryRepository,
            InventoryTransactionRepository inventoryTransactionRepository,
            ProductVariantRepository productVariantRepository,
            UserRepository userRepository
    ) {
        this.inventoryRepository = inventoryRepository;
        this.inventoryTransactionRepository = inventoryTransactionRepository;
        this.productVariantRepository = productVariantRepository;
        this.userRepository = userRepository;
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
    public InventoryResponse importInventory(Long currentUserId, InventoryImportRequest request) {
        if (request.quantity() <= 0) {
            throw new BusinessException(ErrorCode.INVALID_STOCK_QUANTITY, "Số lượng nhập phải lớn hơn 0");
        }

        ProductVariant variant = productVariantRepository.findById(request.variantId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND, "Không tìm thấy biến thể sản phẩm với ID: " + request.variantId()));

        Inventory inventory = inventoryRepository.findByVariantId(request.variantId())
                .orElseGet(() -> {
                    Inventory newInv = new Inventory(variant, 0, 0, 5);
                    return inventoryRepository.save(newInv);
                });

        int newQuantity = inventory.getQuantityOnHand() + request.quantity();
        inventory.setQuantityOnHand(newQuantity);
        inventory.setUpdatedAt(Instant.now());
        inventory = inventoryRepository.save(inventory);

        variant.setStockQuantity(newQuantity);
        productVariantRepository.save(variant);

        User currentUser = null;
        if (currentUserId != null) {
            currentUser = userRepository.findById(currentUserId).orElse(null);
        }

        InventoryTransaction transaction = new InventoryTransaction();
        transaction.setInventory(inventory);
        transaction.setTransactionType(InventoryTransactionType.IMPORT);
        transaction.setQuantityChange(request.quantity());
        transaction.setReferenceType(request.referenceType() != null && !request.referenceType().isBlank() ? request.referenceType() : "MANUAL_IMPORT");
        transaction.setReferenceId(request.referenceId());
        transaction.setNote(request.note());
        transaction.setCreatedBy(currentUser);
        transaction.setCreatedAt(Instant.now());
        inventoryTransactionRepository.save(transaction);

        return InventoryResponse.from(inventory);
    }

    @Override
    @Transactional
    public InventoryResponse adjustInventory(Long currentUserId, InventoryAdjustmentRequest request) {
        if (request.quantityChange() == null || request.quantityChange() == 0) {
            throw new BusinessException(ErrorCode.INVALID_STOCK_QUANTITY, "Số lượng điều chỉnh phải khác 0");
        }
        if (request.reason() == null || request.reason().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Lý do điều chỉnh không được để trống");
        }

        ProductVariant variant = productVariantRepository.findById(request.variantId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND, "Không tìm thấy biến thể sản phẩm với ID: " + request.variantId()));

        Inventory inventory = inventoryRepository.findByVariantId(request.variantId())
                .orElseGet(() -> {
                    Inventory newInv = new Inventory(variant, 0, 0, 5);
                    return inventoryRepository.save(newInv);
                });

        int newQuantity = inventory.getQuantityOnHand() + request.quantityChange();
        if (newQuantity < 0) {
            throw new BusinessException(ErrorCode.INVALID_STOCK_QUANTITY,
                    String.format("Số lượng tồn kho sau điều chỉnh không thể nhỏ hơn 0 (Tồn hiện tại: %d, Điều chỉnh: %d)",
                            inventory.getQuantityOnHand(), request.quantityChange()));
        }
        if (newQuantity < inventory.getQuantityReserved()) {
            throw new BusinessException(ErrorCode.INVALID_STOCK_QUANTITY,
                    String.format("Số lượng tồn kho không thể nhỏ hơn số lượng đang giữ cho đơn hàng (Tồn sau điều chỉnh: %d, Đang giữ: %d)",
                            newQuantity, inventory.getQuantityReserved()));
        }

        inventory.setQuantityOnHand(newQuantity);
        inventory.setUpdatedAt(Instant.now());
        inventory = inventoryRepository.save(inventory);

        variant.setStockQuantity(newQuantity);
        productVariantRepository.save(variant);

        User currentUser = null;
        if (currentUserId != null) {
            currentUser = userRepository.findById(currentUserId).orElse(null);
        }

        InventoryTransaction transaction = new InventoryTransaction();
        transaction.setInventory(inventory);
        transaction.setTransactionType(InventoryTransactionType.ADJUSTMENT);
        transaction.setQuantityChange(request.quantityChange());
        transaction.setReferenceType(request.referenceType() != null && !request.referenceType().isBlank() ? request.referenceType() : "MANUAL_ADJUSTMENT");
        transaction.setReferenceId(request.referenceId());
        transaction.setNote(request.reason().trim());
        transaction.setCreatedBy(currentUser);
        transaction.setCreatedAt(Instant.now());
        inventoryTransactionRepository.save(transaction);

        return InventoryResponse.from(inventory);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<InventoryTransactionResponse> getTransactions(Long variantId, InventoryTransactionType type, Pageable pageable) {
        Page<InventoryTransaction> page = inventoryTransactionRepository.findTransactions(variantId, type, pageable);
        Page<InventoryTransactionResponse> mapped = page.map(tx -> {
            Inventory inv = tx.getInventory();
            ProductVariant v = inv.getVariant();
            var p = v.getProduct();
            User u = tx.getCreatedBy();

            return new InventoryTransactionResponse(
                    tx.getId(),
                    inv.getId(),
                    v.getId(),
                    p != null ? p.getName() : null,
                    v.getSku(),
                    v.getColor(),
                    v.getStorage(),
                    tx.getTransactionType(),
                    tx.getQuantityChange(),
                    tx.getReferenceType(),
                    tx.getReferenceId(),
                    tx.getNote(),
                    u != null ? u.getId() : null,
                    u != null ? u.getFullName() : null,
                    u != null ? u.getEmail() : null,
                    tx.getCreatedAt()
            );
        });
        return PageResponse.of(mapped);
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

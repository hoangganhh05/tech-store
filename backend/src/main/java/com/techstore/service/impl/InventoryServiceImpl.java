package com.techstore.service.impl;

import com.techstore.dto.request.InventoryAdjustmentRequest;
import com.techstore.dto.request.InventoryImportRequest;
import com.techstore.dto.request.OrderInventoryDeductionRequest;
import com.techstore.dto.request.OrderInventoryRestoreRequest;
import com.techstore.dto.request.OrderItemStockRequest;
import com.techstore.dto.request.UpdateThresholdRequest;
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
import com.techstore.enums.VariantStatus;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    @Transactional
    public void deductInventoryForOrder(Long currentUserId, OrderInventoryDeductionRequest request) {
        if (request == null || request.items() == null || request.items().isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Danh sách sản phẩm trong đơn không được để trống");
        }

        Map<Long, Integer> aggregatedQuantities = new HashMap<>();
        for (OrderItemStockRequest item : request.items()) {
            if (item.variantId() == null) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "ID biến thể không được để trống");
            }
            if (item.quantity() == null || item.quantity() <= 0) {
                throw new BusinessException(ErrorCode.INVALID_STOCK_QUANTITY, "Số lượng trừ kho phải lớn hơn 0");
            }
            aggregatedQuantities.merge(item.variantId(), item.quantity(), Integer::sum);
        }

        // Sort variant IDs in ascending order to prevent deadlocks across concurrent orders
        List<Long> sortedVariantIds = aggregatedQuantities.keySet().stream()
                .sorted()
                .toList();

        User currentUser = null;
        if (currentUserId != null) {
            currentUser = userRepository.findById(currentUserId).orElse(null);
        }

        String note = request.note();
        if (note == null || note.isBlank()) {
            note = "Xuất kho đơn hàng " + (request.orderCode() != null ? request.orderCode() : (request.orderId() != null ? "#" + request.orderId() : ""));
        }

        for (Long variantId : sortedVariantIds) {
            int quantityToDeduct = aggregatedQuantities.get(variantId);

            // Pessimistic write lock on inventory row
            Inventory inventory = inventoryRepository.findByVariantIdWithLock(variantId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.INVENTORY_NOT_FOUND,
                            "Không tìm thấy thông tin tồn kho cho biến thể ID: " + variantId));

            ProductVariant variant = inventory.getVariant();
            if (variant == null || variant.isDeleted()) {
                throw new BusinessException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND,
                        "Biến thể sản phẩm với ID " + variantId + " không tồn tại hoặc đã bị xoá");
            }
            if (variant.getStatus() != VariantStatus.ACTIVE) {
                throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK,
                        String.format("Biến thể '%s' (%s) hiện đang ngừng kinh doanh",
                                variant.getProduct() != null ? variant.getProduct().getName() : "Sản phẩm",
                                variant.getSku()));
            }

            int availableQuantity = inventory.getAvailableQuantity();
            if (availableQuantity < quantityToDeduct) {
                String productName = (variant.getProduct() != null) ? variant.getProduct().getName() : "Sản phẩm";
                throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK,
                        String.format("Sản phẩm '%s' (SKU: %s) không đủ tồn kho khả dụng. Khả dụng: %d, yêu cầu: %d",
                                productName, variant.getSku(), availableQuantity, quantityToDeduct));
            }

            int newOnHand = inventory.getQuantityOnHand() - quantityToDeduct;
            inventory.setQuantityOnHand(newOnHand);
            inventory.setUpdatedAt(Instant.now());
            inventoryRepository.save(inventory);

            variant.setStockQuantity(newOnHand);
            productVariantRepository.save(variant);

            InventoryTransaction transaction = new InventoryTransaction();
            transaction.setInventory(inventory);
            transaction.setTransactionType(InventoryTransactionType.SALE);
            transaction.setQuantityChange(-quantityToDeduct);
            transaction.setReferenceType("ORDER");
            transaction.setReferenceId(request.orderId());
            transaction.setNote(note.trim());
            transaction.setCreatedBy(currentUser);
            transaction.setCreatedAt(Instant.now());
            inventoryTransactionRepository.save(transaction);
        }
    }

    @Override
    @Transactional
    public void restoreInventoryForOrder(Long currentUserId, OrderInventoryRestoreRequest request) {
        if (request == null || request.items() == null || request.items().isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Danh sách sản phẩm hoàn kho không được để trống");
        }
        if (request.reason() == null || request.reason().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Lý do hoàn tồn kho không được để trống");
        }

        Map<Long, Integer> aggregatedQuantities = new HashMap<>();
        for (OrderItemStockRequest item : request.items()) {
            if (item.variantId() == null) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "ID biến thể không được để trống");
            }
            if (item.quantity() == null || item.quantity() <= 0) {
                throw new BusinessException(ErrorCode.INVALID_STOCK_QUANTITY, "Số lượng hoàn kho phải lớn hơn 0");
            }
            aggregatedQuantities.merge(item.variantId(), item.quantity(), Integer::sum);
        }

        // Sort variant IDs in ascending order to prevent deadlocks
        List<Long> sortedVariantIds = aggregatedQuantities.keySet().stream()
                .sorted()
                .toList();

        User currentUser = null;
        if (currentUserId != null) {
            currentUser = userRepository.findById(currentUserId).orElse(null);
        }

        String note = request.reason().trim();

        for (Long variantId : sortedVariantIds) {
            int quantityToRestore = aggregatedQuantities.get(variantId);

            Inventory inventory = inventoryRepository.findByVariantIdWithLock(variantId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.INVENTORY_NOT_FOUND,
                            "Không tìm thấy thông tin tồn kho cho biến thể ID: " + variantId));

            int newOnHand = inventory.getQuantityOnHand() + quantityToRestore;
            inventory.setQuantityOnHand(newOnHand);
            inventory.setUpdatedAt(Instant.now());
            inventoryRepository.save(inventory);

            ProductVariant variant = inventory.getVariant();
            if (variant != null) {
                variant.setStockQuantity(newOnHand);
                productVariantRepository.save(variant);
            }

            InventoryTransaction transaction = new InventoryTransaction();
            transaction.setInventory(inventory);
            transaction.setTransactionType(InventoryTransactionType.CANCEL_RETURN);
            transaction.setQuantityChange(quantityToRestore);
            transaction.setReferenceType("ORDER");
            transaction.setReferenceId(request.orderId());
            transaction.setNote(note);
            transaction.setCreatedBy(currentUser);
            transaction.setCreatedAt(Instant.now());
            inventoryTransactionRepository.save(transaction);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<InventoryResponse> getLowStockInventories(String search, Long categoryId, Pageable pageable) {
        String trimmedSearch = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        Page<Inventory> page = inventoryRepository.findLowStockWithFilters(trimmedSearch, categoryId, pageable);
        return PageResponse.of(page.map(InventoryResponse::from));
    }

    @Override
    @Transactional
    public InventoryResponse updateLowStockThreshold(Long variantId, UpdateThresholdRequest request) {
        if (request == null || request.lowStockThreshold() == null || request.lowStockThreshold() < 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Ngưỡng tồn kho thấp phải lớn hơn hoặc bằng 0");
        }

        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND,
                        "Không tìm thấy biến thể sản phẩm với ID: " + variantId));

        Inventory inventory = inventoryRepository.findByVariantId(variantId)
                .orElseGet(() -> {
                    Inventory newInv = new Inventory(variant, 0, 0, request.lowStockThreshold());
                    return inventoryRepository.save(newInv);
                });

        inventory.setLowStockThreshold(request.lowStockThreshold());
        inventory.setUpdatedAt(Instant.now());
        inventory = inventoryRepository.save(inventory);

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

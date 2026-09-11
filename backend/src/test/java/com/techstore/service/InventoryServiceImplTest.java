package com.techstore.service;

import com.techstore.dto.request.InventoryAdjustmentRequest;
import com.techstore.dto.request.InventoryImportRequest;
import com.techstore.dto.request.OrderInventoryDeductionRequest;
import com.techstore.dto.request.OrderInventoryRestoreRequest;
import com.techstore.dto.request.OrderItemStockRequest;
import com.techstore.entity.Inventory;
import com.techstore.entity.InventoryTransaction;
import com.techstore.entity.ProductVariant;
import com.techstore.enums.ErrorCode;
import com.techstore.enums.InventoryTransactionType;
import com.techstore.enums.VariantStatus;
import com.techstore.exception.BusinessException;
import com.techstore.repository.InventoryRepository;
import com.techstore.repository.InventoryTransactionRepository;
import com.techstore.repository.ProductVariantRepository;
import com.techstore.repository.UserRepository;
import com.techstore.service.impl.InventoryServiceImpl;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class InventoryServiceImplTest {
    private final InventoryRepository inventories = mock(InventoryRepository.class);
    private final InventoryTransactionRepository transactions = mock(InventoryTransactionRepository.class);
    private final ProductVariantRepository variants = mock(ProductVariantRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final EntityManager entityManager = mock(EntityManager.class);
    private final InventoryServiceImpl service = new InventoryServiceImpl(inventories, transactions, variants, users);
    private final ProductVariant variant = mock(ProductVariant.class);
    private Inventory inventory;

    @BeforeEach
    void setUp() throws Exception {
        Field field = InventoryServiceImpl.class.getDeclaredField("entityManager");
        field.setAccessible(true);
        field.set(service, entityManager);
        when(variant.getId()).thenReturn(10L);
        when(variant.getSku()).thenReturn("SKU-10");
        when(variant.getStatus()).thenReturn(VariantStatus.ACTIVE);
        when(variant.isDeleted()).thenReturn(false);
        inventory = new Inventory(variant, 10, 2, 5);
        when(variants.findById(10L)).thenReturn(Optional.of(variant));
        when(inventories.findByVariantId(10L)).thenReturn(Optional.of(inventory));
        when(inventories.findByVariantIdWithLock(10L)).thenReturn(Optional.of(inventory));
        when(inventories.save(any(Inventory.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactions.save(any(InventoryTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void importsStockAndRecordsImportTransaction() {
        var response = service.importInventory(null, new InventoryImportRequest(10L, 5, "new stock", null, 77L));

        var savedInventory = captureInventory();
        assertThat(savedInventory.getQuantityOnHand()).isEqualTo(15);
        verify(variant).setStockQuantity(15);
        var tx = captureTransaction();
        assertThat(tx.getTransactionType()).isEqualTo(InventoryTransactionType.IMPORT);
        assertThat(tx.getQuantityChange()).isEqualTo(5);
        assertThat(tx.getReferenceType()).isEqualTo("MANUAL_IMPORT");
    }

    @Test
    void rejectsAdjustmentThatWouldFallBelowReservedQuantity() {
        assertThatThrownBy(() -> service.adjustInventory(null,
                new InventoryAdjustmentRequest(10L, -9, "cycle count", null, null)))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_STOCK_QUANTITY))
                .hasMessageContaining("đang giữ");
        verify(inventories, never()).save(any());
        verifyNoInteractions(transactions);
    }

    @Test
    void deductsDuplicateLinesAsOneLockedSaleTransaction() {
        service.deductInventoryForOrder(null, new OrderInventoryDeductionRequest(55L, "TS-55",
                List.of(new OrderItemStockRequest(10L, 2), new OrderItemStockRequest(10L, 3)), ""));

        var savedInventory = captureInventory();
        assertThat(savedInventory.getQuantityOnHand()).isEqualTo(5);
        verify(variant).setStockQuantity(5);
        verify(entityManager).refresh(inventory);
        verify(inventories).findByVariantIdWithLock(10L);
        var tx = captureTransaction();
        assertThat(tx.getTransactionType()).isEqualTo(InventoryTransactionType.SALE);
        assertThat(tx.getQuantityChange()).isEqualTo(-5);
        assertThat(tx.getNote()).contains("TS-55");
    }

    @Test
    void restoresStockAndCreatesCancelReturnTransaction() {
        service.restoreInventoryForOrder(null, new OrderInventoryRestoreRequest(55L, "TS-55",
                List.of(new OrderItemStockRequest(10L, 4)), "customer cancelled"));

        var savedInventory = captureInventory();
        assertThat(savedInventory.getQuantityOnHand()).isEqualTo(14);
        verify(variant).setStockQuantity(14);
        var tx = captureTransaction();
        assertThat(tx.getTransactionType()).isEqualTo(InventoryTransactionType.CANCEL_RETURN);
        assertThat(tx.getQuantityChange()).isEqualTo(4);
        assertThat(tx.getNote()).isEqualTo("customer cancelled");
    }

    private InventoryTransaction captureTransaction() {
        var captor = org.mockito.ArgumentCaptor.forClass(InventoryTransaction.class);
        verify(transactions).save(captor.capture());
        return captor.getValue();
    }

    private Inventory captureInventory() {
        var captor = org.mockito.ArgumentCaptor.forClass(Inventory.class);
        verify(inventories).save(captor.capture());
        return captor.getValue();
    }
}

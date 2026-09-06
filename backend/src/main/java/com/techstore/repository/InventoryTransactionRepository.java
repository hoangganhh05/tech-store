package com.techstore.repository;

import com.techstore.entity.InventoryTransaction;
import com.techstore.enums.InventoryTransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Long> {

    List<InventoryTransaction> findByInventoryIdOrderByCreatedAtDesc(Long inventoryId);

    Page<InventoryTransaction> findByInventoryId(Long inventoryId, Pageable pageable);

    @Query(value = "SELECT it FROM InventoryTransaction it " +
            "JOIN FETCH it.inventory i " +
            "JOIN FETCH i.variant v " +
            "JOIN FETCH v.product p " +
            "LEFT JOIN FETCH it.createdBy u " +
            "WHERE (:variantId IS NULL OR v.id = :variantId) " +
            "AND (:type IS NULL OR it.transactionType = :type)",
            countQuery = "SELECT count(it) FROM InventoryTransaction it " +
                    "JOIN it.inventory i " +
                    "WHERE (:variantId IS NULL OR i.variant.id = :variantId) " +
                    "AND (:type IS NULL OR it.transactionType = :type)")
    Page<InventoryTransaction> findTransactions(
            @Param("variantId") Long variantId,
            @Param("type") InventoryTransactionType type,
            Pageable pageable
    );
}

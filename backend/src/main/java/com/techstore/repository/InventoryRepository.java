package com.techstore.repository;

import com.techstore.entity.Inventory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.QueryHints;

import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findByVariantId(Long variantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000")})
    @Query("SELECT i FROM Inventory i WHERE i.variant.id = :variantId")
    Optional<Inventory> findByVariantIdWithLock(@Param("variantId") Long variantId);

    @Query(value = """
            SELECT i FROM Inventory i
            JOIN i.variant pv
            JOIN pv.product p
            LEFT JOIN p.category c
            LEFT JOIN p.brand b
            WHERE pv.isDeleted = false
              AND p.isDeleted = false
              AND (:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(pv.sku) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:categoryId IS NULL OR c.id = :categoryId)
              AND (
                   :statusFilter = 'ALL'
                   OR (:statusFilter = 'IN_STOCK' AND (i.quantityOnHand - i.quantityReserved) > i.lowStockThreshold)
                   OR (:statusFilter = 'LOW_STOCK' AND (i.quantityOnHand - i.quantityReserved) > 0 AND (i.quantityOnHand - i.quantityReserved) <= i.lowStockThreshold)
                   OR (:statusFilter = 'OUT_OF_STOCK' AND (i.quantityOnHand - i.quantityReserved) <= 0)
              )
            """,
            countQuery = """
            SELECT COUNT(i) FROM Inventory i
            JOIN i.variant pv
            JOIN pv.product p
            LEFT JOIN p.category c
            WHERE pv.isDeleted = false
              AND p.isDeleted = false
              AND (:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(pv.sku) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:categoryId IS NULL OR c.id = :categoryId)
              AND (
                   :statusFilter = 'ALL'
                   OR (:statusFilter = 'IN_STOCK' AND (i.quantityOnHand - i.quantityReserved) > i.lowStockThreshold)
                   OR (:statusFilter = 'LOW_STOCK' AND (i.quantityOnHand - i.quantityReserved) > 0 AND (i.quantityOnHand - i.quantityReserved) <= i.lowStockThreshold)
                   OR (:statusFilter = 'OUT_OF_STOCK' AND (i.quantityOnHand - i.quantityReserved) <= 0)
              )
            """)
    Page<Inventory> findWithFilters(
            @Param("search") String search,
            @Param("categoryId") Long categoryId,
            @Param("statusFilter") String statusFilter,
            Pageable pageable
    );

    @Query(value = """
            SELECT i FROM Inventory i
            JOIN i.variant pv
            JOIN pv.product p
            LEFT JOIN p.category c
            LEFT JOIN p.brand b
            WHERE pv.isDeleted = false
              AND p.isDeleted = false
              AND (i.quantityOnHand - i.quantityReserved) <= i.lowStockThreshold
              AND (:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(pv.sku) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:categoryId IS NULL OR c.id = :categoryId)
            """,
            countQuery = """
            SELECT COUNT(i) FROM Inventory i
            JOIN i.variant pv
            JOIN pv.product p
            LEFT JOIN p.category c
            WHERE pv.isDeleted = false
              AND p.isDeleted = false
              AND (i.quantityOnHand - i.quantityReserved) <= i.lowStockThreshold
              AND (:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(pv.sku) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:categoryId IS NULL OR c.id = :categoryId)
            """)
    Page<Inventory> findLowStockWithFilters(
            @Param("search") String search,
            @Param("categoryId") Long categoryId,
            Pageable pageable
    );

    @Query("""
            SELECT COUNT(i) FROM Inventory i
            JOIN i.variant pv
            JOIN pv.product p
            WHERE pv.isDeleted = false AND p.isDeleted = false
            """)
    long countActiveVariants();

    @Query("""
            SELECT COUNT(i) FROM Inventory i
            JOIN i.variant pv
            JOIN pv.product p
            WHERE pv.isDeleted = false AND p.isDeleted = false
              AND (i.quantityOnHand - i.quantityReserved) > i.lowStockThreshold
            """)
    long countInStock();

    @Query("""
            SELECT COUNT(i) FROM Inventory i
            JOIN i.variant pv
            JOIN pv.product p
            WHERE pv.isDeleted = false AND p.isDeleted = false
              AND (i.quantityOnHand - i.quantityReserved) > 0
              AND (i.quantityOnHand - i.quantityReserved) <= i.lowStockThreshold
            """)
    long countLowStock();

    @Query("""
            SELECT COUNT(i) FROM Inventory i
            JOIN i.variant pv
            JOIN pv.product p
            WHERE pv.isDeleted = false AND p.isDeleted = false
              AND (i.quantityOnHand - i.quantityReserved) <= 0
            """)
    long countOutOfStock();
}

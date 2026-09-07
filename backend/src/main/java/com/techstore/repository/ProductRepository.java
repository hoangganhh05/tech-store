package com.techstore.repository;

import com.techstore.entity.Product;
import com.techstore.enums.InventoryTransactionType;
import com.techstore.enums.ProductStatus;
import com.techstore.enums.VariantStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    boolean existsByNameIgnoreCaseAndBrandId(String name, Long brandId);

    boolean existsByNameIgnoreCaseAndBrandIdAndIdNot(String name, Long brandId, Long id);

    boolean existsByNameIgnoreCaseAndBrandIdAndIsDeletedFalse(String name, Long brandId);

    boolean existsByNameIgnoreCaseAndBrandIdAndIdNotAndIsDeletedFalse(String name, Long brandId, Long id);

    boolean existsByBrandId(Long brandId);

    boolean existsByCategoryId(Long categoryId);

    List<Product> findAllByOrderByCreatedAtDesc();

    List<Product> findAllByIsDeletedFalseOrderByCreatedAtDesc();

    Optional<Product> findByIdAndIsDeletedFalse(Long id);

    List<Product> findByCategoryId(Long categoryId);

    List<Product> findByBrandId(Long brandId);

    List<Product> findByStatusOrderByCreatedAtDesc(ProductStatus status);

    List<Product> findByStatusAndIsDeletedFalseOrderByCreatedAtDesc(ProductStatus status);

    List<Product> findByStatusAndIsDeletedFalseOrderByCreatedAtDesc(ProductStatus status, Pageable pageable);

    @Query("SELECT p FROM Product p " +
            "WHERE p.status = :status AND p.isDeleted = false " +
            "ORDER BY (SELECT COALESCE(SUM(ABS(it.quantityChange)), 0) " +
            "          FROM InventoryTransaction it " +
            "          WHERE it.inventory.variant.product = p " +
            "          AND it.transactionType = :saleType) DESC, p.createdAt DESC")
    List<Product> findFeaturedProducts(
            @Param("status") ProductStatus status,
            @Param("saleType") InventoryTransactionType saleType,
            Pageable pageable
    );

    @Query("SELECT DISTINCT p FROM Product p " +
            "JOIN ProductVariant pv ON pv.product = p " +
            "WHERE p.status = :status AND p.isDeleted = false " +
            "AND pv.status = :variantStatus AND pv.isDeleted = false " +
            "AND pv.originalPrice IS NOT NULL AND pv.originalPrice > pv.price " +
            "ORDER BY p.createdAt DESC")
    List<Product> findOnSaleProducts(
            @Param("status") ProductStatus status,
            @Param("variantStatus") VariantStatus variantStatus,
            Pageable pageable
    );

    @Query("SELECT p FROM Product p " +
            "WHERE p.status = :status AND p.isDeleted = false " +
            "AND (p.category.id = :categoryId OR p.category.parent.id = :categoryId) " +
            "ORDER BY p.createdAt DESC")
    List<Product> findByCategoryOrParentCategoryIdAndStatus(
            @Param("categoryId") Long categoryId,
            @Param("status") ProductStatus status
    );
}


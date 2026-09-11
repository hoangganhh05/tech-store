package com.techstore.repository;

import com.techstore.entity.Product;
import com.techstore.enums.InventoryTransactionType;
import com.techstore.enums.ProductStatus;
import com.techstore.enums.VariantStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
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

    @EntityGraph(attributePaths = {"brand", "category"})
    List<Product> findByStatusAndIsDeletedFalseOrderByCreatedAtDesc(ProductStatus status);

    @EntityGraph(attributePaths = {"brand", "category"})
    List<Product> findByStatusAndIsDeletedFalseOrderByCreatedAtDesc(ProductStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"brand", "category"})
    List<Product> findByIdInAndStatusAndIsDeletedFalse(Collection<Long> productIds, ProductStatus status);

    @EntityGraph(attributePaths = {"brand", "category"})
    List<Product> findByStatusAndIsDeletedFalseAndBrandIdInOrderByCreatedAtDesc(
            ProductStatus status,
            Collection<Long> brandIds
    );

    @EntityGraph(attributePaths = {"brand", "category"})
    @Query("""
            SELECT p FROM Product p
            WHERE p.status = :status AND p.isDeleted = false
              AND (p.category.id = :categoryId OR p.category.parent.id = :categoryId)
              AND p.brand.id IN :brandIds
            ORDER BY p.createdAt DESC
            """)
    List<Product> findByCategoryOrParentCategoryIdAndStatusAndBrandIdIn(
            @Param("categoryId") Long categoryId,
            @Param("status") ProductStatus status,
            @Param("brandIds") Collection<Long> brandIds
    );

    @EntityGraph(attributePaths = {"brand", "category"})
    @Query("""
            SELECT p FROM Product p
            WHERE p.status = :status AND p.isDeleted = false
              AND (
                    LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                 OR LOWER(COALESCE(p.description, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                 OR LOWER(p.brand.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
            ORDER BY p.createdAt DESC
            """)
    List<Product> findActiveSearchCandidates(
            @Param("keyword") String keyword,
            @Param("status") ProductStatus status
    );

    @EntityGraph(attributePaths = {"brand", "category"})
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

    @EntityGraph(attributePaths = {"brand", "category"})
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

    @EntityGraph(attributePaths = {"brand", "category"})
    @Query("SELECT p FROM Product p " +
            "WHERE p.status = :status AND p.isDeleted = false " +
            "AND (p.category.id = :categoryId OR p.category.parent.id = :categoryId) " +
            "ORDER BY p.createdAt DESC")
    List<Product> findByCategoryOrParentCategoryIdAndStatus(
            @Param("categoryId") Long categoryId,
            @Param("status") ProductStatus status
    );

    @Query("SELECT p FROM Product p " +
            "WHERE p.status = :status AND p.isDeleted = false " +
            "AND p.id != :excludeId " +
            "AND ((:categoryId IS NOT NULL AND (p.category.id = :categoryId OR p.category.parent.id = :categoryId)) " +
            "     OR (:brandId IS NOT NULL AND p.brand.id = :brandId)) " +
            "ORDER BY " +
            "  CASE WHEN (:categoryId IS NOT NULL AND (p.category.id = :categoryId OR p.category.parent.id = :categoryId)) " +
            "            AND (:brandId IS NOT NULL AND p.brand.id = :brandId) THEN 0 " +
            "       WHEN (:categoryId IS NOT NULL AND (p.category.id = :categoryId OR p.category.parent.id = :categoryId)) THEN 1 " +
            "       ELSE 2 END ASC, " +
            "  p.createdAt DESC")
    @EntityGraph(attributePaths = {"brand", "category"})
    List<Product> findRelatedProducts(
            @Param("excludeId") Long excludeId,
            @Param("categoryId") Long categoryId,
            @Param("brandId") Long brandId,
            @Param("status") ProductStatus status,
            Pageable pageable
    );
}


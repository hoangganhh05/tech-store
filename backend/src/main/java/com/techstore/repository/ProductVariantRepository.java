package com.techstore.repository;

import com.techstore.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    List<ProductVariant> findByProductId(Long productId);

    List<ProductVariant> findByProductIdOrderByCreatedAtAsc(Long productId);

    List<ProductVariant> findByProductIdAndIsDeletedFalseOrderByCreatedAtAsc(Long productId);

    List<ProductVariant> findByProductIdInAndIsDeletedFalseOrderByCreatedAtAsc(java.util.Collection<Long> productIds);

    Optional<ProductVariant> findByIdAndProductId(Long id, Long productId);

    Optional<ProductVariant> findByIdAndProductIdAndIsDeletedFalse(Long id, Long productId);

    boolean existsBySkuIgnoreCase(String sku);

    boolean existsBySkuIgnoreCaseAndIdNot(String sku, Long id);

    long countByProductId(Long productId);

    long countByProductIdAndIsDeletedFalse(Long productId);
}

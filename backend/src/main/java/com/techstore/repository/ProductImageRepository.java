package com.techstore.repository;

import com.techstore.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    List<ProductImage> findByProductIdOrderByIsPrimaryDescDisplayOrderAscIdAsc(Long productId);

    Optional<ProductImage> findByIdAndProductId(Long id, Long productId);

    boolean existsByProductIdAndIsPrimaryTrue(Long productId);

    long countByProductId(Long productId);

    @Modifying
    @Query("UPDATE ProductImage img SET img.isPrimary = false WHERE img.product.id = :productId")
    void demotePrimaryForProduct(@Param("productId") Long productId);

    List<ProductImage> findByVariantId(Long variantId);
}

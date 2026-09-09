package com.techstore.repository;

import com.techstore.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    Optional<Review> findByUserIdAndProductId(Long userId, Long productId);

    boolean existsByUserIdAndProductId(Long userId, Long productId);

    @Query("""
        SELECT COUNT(o) > 0
        FROM Order o
        JOIN o.items oi
        JOIN ProductVariant pv ON oi.variantId = pv.id
        WHERE o.user.id = :userId
          AND o.status = 'COMPLETED'
          AND pv.product.id = :productId
    """)
    boolean hasCompletedPurchase(@Param("userId") Long userId, @Param("productId") Long productId);
}


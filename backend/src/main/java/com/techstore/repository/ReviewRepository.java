package com.techstore.repository;

import com.techstore.entity.Review;
import com.techstore.enums.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    Optional<Review> findByUserIdAndProductId(Long userId, Long productId);

    boolean existsByUserIdAndProductId(Long userId, Long productId);

    Page<Review> findByProductIdAndStatus(Long productId, ReviewStatus status, Pageable pageable);

    long countByProductIdAndStatus(Long productId, ReviewStatus status);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.product.id = :productId AND r.status = :status")
    Double findAverageRatingByProductIdAndStatus(
            @Param("productId") Long productId,
            @Param("status") ReviewStatus status
    );

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


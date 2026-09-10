package com.techstore.repository;

import com.techstore.entity.Product;
import com.techstore.entity.Wishlist;
import com.techstore.enums.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

    boolean existsByUserIdAndProductId(Long userId, Long productId);

    Optional<Wishlist> findByUserIdAndProductId(Long userId, Long productId);

    void deleteByUserIdAndProductId(Long userId, Long productId);

    @Query(value = "SELECT p FROM Wishlist w JOIN w.product p "
            + "WHERE w.user.id = :userId AND p.status = :status AND p.isDeleted = false "
            + "ORDER BY w.createdAt DESC, w.id DESC",
            countQuery = "SELECT COUNT(w) FROM Wishlist w JOIN w.product p "
                    + "WHERE w.user.id = :userId AND p.status = :status AND p.isDeleted = false")
    Page<Product> findActiveProductsByUserId(
            @Param("userId") Long userId,
            @Param("status") ProductStatus status,
            Pageable pageable
    );
}

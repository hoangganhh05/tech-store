package com.techstore.repository;

import com.techstore.entity.Promotion;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface PromotionRepository extends JpaRepository<Promotion, Long> {

    @EntityGraph(attributePaths = {"product", "category", "variant", "variant.product"})
    @Query("select p from Promotion p where p.active = true and p.startsAt <= :now and p.endsAt >= :now")
    List<Promotion> findActiveAt(@Param("now") Instant now);
}

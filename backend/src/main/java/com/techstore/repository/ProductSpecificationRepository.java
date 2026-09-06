package com.techstore.repository;

import com.techstore.entity.ProductSpecification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductSpecificationRepository extends JpaRepository<ProductSpecification, Long> {

    List<ProductSpecification> findByProductIdOrderByDisplayOrderAscIdAsc(Long productId);

    Optional<ProductSpecification> findByIdAndProductId(Long id, Long productId);

    boolean existsByProductIdAndSpecKeyIgnoreCase(Long productId, String specKey);

    boolean existsByProductIdAndSpecKeyIgnoreCaseAndIdNot(Long productId, String specKey, Long id);

    void deleteByProductId(Long productId);
}

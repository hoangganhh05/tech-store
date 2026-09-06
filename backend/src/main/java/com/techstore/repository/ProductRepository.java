package com.techstore.repository;

import com.techstore.entity.Product;
import com.techstore.enums.ProductStatus;
import org.springframework.data.jpa.repository.JpaRepository;
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
}

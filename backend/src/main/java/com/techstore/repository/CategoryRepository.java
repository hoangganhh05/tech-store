package com.techstore.repository;

import com.techstore.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    boolean existsByNameIgnoreCaseAndParentIsNull(String name);

    boolean existsByNameIgnoreCaseAndParentId(String name, Long parentId);

    boolean existsByNameIgnoreCaseAndParentIsNullAndIdNot(String name, Long id);

    boolean existsByNameIgnoreCaseAndParentIdAndIdNot(String name, Long parentId, Long id);

    boolean existsByParentId(Long parentId);

    List<Category> findByParentIsNullOrderByNameAsc();

    List<Category> findAllByOrderByNameAsc();
}
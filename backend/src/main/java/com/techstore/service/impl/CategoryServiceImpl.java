package com.techstore.service.impl;

import com.techstore.dto.request.CategoryRequest;
import com.techstore.dto.response.CategoryResponse;
import com.techstore.dto.response.CategoryTreeResponse;
import com.techstore.entity.Category;
import com.techstore.enums.ErrorCode;
import com.techstore.exception.BusinessException;
import com.techstore.repository.CategoryRepository;
import com.techstore.service.CategoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryServiceImpl(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        String trimmedName = request.name().trim();
        Category parent = null;

        if (request.parentId() != null) {
            parent = categoryRepository.findById(request.parentId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND, "Danh mục cha không tồn tại"));

            if (categoryRepository.existsByNameIgnoreCaseAndParentId(trimmedName, request.parentId())) {
                throw new BusinessException(ErrorCode.CATEGORY_NAME_DUPLICATE, "Tên danh mục đã tồn tại trong cùng một cấp");
            }
        } else {
            if (categoryRepository.existsByNameIgnoreCaseAndParentIsNull(trimmedName)) {
                throw new BusinessException(ErrorCode.CATEGORY_NAME_DUPLICATE, "Tên danh mục đã tồn tại trong cùng một cấp");
            }
        }

        Category category = new Category(
                trimmedName,
                request.description() != null ? request.description().trim() : null,
                parent,
                request.imageUrl() != null ? request.imageUrl().trim() : null
        );

        Category saved = categoryRepository.save(category);
        return toCategoryResponse(saved);
    }

    @Override
    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND, "Không tìm thấy danh mục"));

        String trimmedName = request.name().trim();
        Category newParent = null;

        if (request.parentId() != null) {
            if (Objects.equals(request.parentId(), id)) {
                throw new BusinessException(ErrorCode.CATEGORY_CANNOT_BE_OWN_PARENT, "Danh mục không thể làm cha của chính nó");
            }

            newParent = categoryRepository.findById(request.parentId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND, "Danh mục cha không tồn tại"));

            // Check circular dependency: newParent cannot be a descendant of current category
            Category ancestor = newParent;
            while (ancestor != null) {
                if (Objects.equals(ancestor.getId(), id)) {
                    throw new BusinessException(ErrorCode.CATEGORY_CANNOT_BE_OWN_PARENT, "Không thể chọn danh mục con làm danh mục cha");
                }
                ancestor = ancestor.getParent();
            }

            if (categoryRepository.existsByNameIgnoreCaseAndParentIdAndIdNot(trimmedName, request.parentId(), id)) {
                throw new BusinessException(ErrorCode.CATEGORY_NAME_DUPLICATE, "Tên danh mục đã tồn tại trong cùng một cấp");
            }
        } else {
            if (categoryRepository.existsByNameIgnoreCaseAndParentIsNullAndIdNot(trimmedName, id)) {
                throw new BusinessException(ErrorCode.CATEGORY_NAME_DUPLICATE, "Tên danh mục đã tồn tại trong cùng một cấp");
            }
        }

        category.update(
                trimmedName,
                request.description() != null ? request.description().trim() : null,
                newParent,
                request.imageUrl() != null ? request.imageUrl().trim() : null
        );

        Category updated = categoryRepository.save(category);
        return toCategoryResponse(updated);
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND, "Không tìm thấy danh mục"));

        if (categoryRepository.existsByParentId(id)) {
            throw new BusinessException(ErrorCode.CATEGORY_HAS_CHILDREN, "Không thể xoá danh mục đang có danh mục con gắn với nó");
        }

        categoryRepository.delete(category);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAllByOrderByNameAsc()
                .stream()
                .map(this::toCategoryResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryTreeResponse> getCategoryTree() {
        List<Category> rootCategories = categoryRepository.findByParentIsNullOrderByNameAsc();
        return rootCategories.stream()
                .map(this::toCategoryTreeResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND, "Không tìm thấy danh mục"));
        return toCategoryResponse(category);
    }

    private CategoryResponse toCategoryResponse(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.getParent() != null ? category.getParent().getId() : null,
                category.getParent() != null ? category.getParent().getName() : null,
                category.getImageUrl(),
                category.getCreatedAt(),
                category.getUpdatedAt()
        );
    }

    private CategoryTreeResponse toCategoryTreeResponse(Category category) {
        List<CategoryTreeResponse> children = category.getChildren() != null
                ? category.getChildren().stream().map(this::toCategoryTreeResponse).toList()
                : new ArrayList<>();

        return new CategoryTreeResponse(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.getParent() != null ? category.getParent().getId() : null,
                category.getImageUrl(),
                children
        );
    }
}
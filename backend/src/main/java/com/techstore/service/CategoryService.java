package com.techstore.service;

import com.techstore.dto.request.CategoryRequest;
import com.techstore.dto.response.CategoryResponse;
import com.techstore.dto.response.CategoryTreeResponse;

import java.util.List;

public interface CategoryService {

    CategoryResponse createCategory(CategoryRequest request);

    CategoryResponse updateCategory(Long id, CategoryRequest request);

    void deleteCategory(Long id);

    List<CategoryResponse> getAllCategories();

    List<CategoryTreeResponse> getCategoryTree();

    CategoryResponse getCategoryById(Long id);
}
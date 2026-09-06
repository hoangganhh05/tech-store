package com.techstore.controller;

import com.techstore.dto.response.ApiResponse;
import com.techstore.dto.response.CategoryTreeResponse;
import com.techstore.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("${app.api.base-path}/categories")
@Tag(name = "Category", description = "Public category endpoints for storefront")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    @Operation(summary = "Get active categories tree for storefront")
    public ResponseEntity<ApiResponse<List<CategoryTreeResponse>>> getPublicCategoryTree() {
        List<CategoryTreeResponse> response = categoryService.getPublicCategoryTree();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh mục thành công", response));
    }
}

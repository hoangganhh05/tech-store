package com.techstore.controller;

import com.techstore.dto.response.ApiResponse;
import com.techstore.dto.response.BrandResponse;
import com.techstore.dto.response.CategoryResponse;
import com.techstore.dto.response.StorefrontHomeResponse;
import com.techstore.dto.response.StorefrontProductResponse;
import com.techstore.service.StorefrontProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("${app.api.base-path}/storefront")
@Validated
@Tag(name = "Storefront", description = "Public storefront home and discovery endpoints")
public class StorefrontController {

    private final StorefrontProductService storefrontProductService;

    public StorefrontController(StorefrontProductService storefrontProductService) {
        this.storefrontProductService = storefrontProductService;
    }

    @GetMapping("/home")
    @Operation(summary = "Get aggregated data for storefront home page")
    public ResponseEntity<ApiResponse<StorefrontHomeResponse>> getHomeData(
            @RequestParam(defaultValue = "8")
            @Min(value = 1, message = "Số lượng sản phẩm tối thiểu là 1")
            @Max(value = 50, message = "Số lượng sản phẩm tối đa là 50")
            int limit
    ) {
        StorefrontHomeResponse response = storefrontProductService.getHomeData(limit);
        return ResponseEntity.ok(ApiResponse.success("Lấy dữ liệu trang chủ thành công", response));
    }

    @GetMapping("/featured")
    @Operation(summary = "Get featured products for storefront")
    public ResponseEntity<ApiResponse<List<StorefrontProductResponse>>> getFeatured(
            @RequestParam(defaultValue = "8")
            @Min(value = 1, message = "Số lượng sản phẩm tối thiểu là 1")
            @Max(value = 50, message = "Số lượng sản phẩm tối đa là 50")
            int limit
    ) {
        List<StorefrontProductResponse> response = storefrontProductService.getFeaturedProducts(limit);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách sản phẩm nổi bật thành công", response));
    }

    @GetMapping("/new-arrivals")
    @Operation(summary = "Get new arrival products for storefront")
    public ResponseEntity<ApiResponse<List<StorefrontProductResponse>>> getNewArrivals(
            @RequestParam(defaultValue = "8")
            @Min(value = 1, message = "Số lượng sản phẩm tối thiểu là 1")
            @Max(value = 50, message = "Số lượng sản phẩm tối đa là 50")
            int limit
    ) {
        List<StorefrontProductResponse> response = storefrontProductService.getNewArrivals(limit);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách sản phẩm mới thành công", response));
    }

    @GetMapping("/on-sale")
    @Operation(summary = "Get on sale products for storefront")
    public ResponseEntity<ApiResponse<List<StorefrontProductResponse>>> getOnSale(
            @RequestParam(defaultValue = "8")
            @Min(value = 1, message = "Số lượng sản phẩm tối thiểu là 1")
            @Max(value = 50, message = "Số lượng sản phẩm tối đa là 50")
            int limit
    ) {
        List<StorefrontProductResponse> response = storefrontProductService.getOnSaleProducts(limit);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách sản phẩm khuyến mãi thành công", response));
    }

    @GetMapping("/categories")
    @Operation(summary = "Get featured categories for storefront")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getFeaturedCategories() {
        List<CategoryResponse> response = storefrontProductService.getFeaturedCategories();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh mục nổi bật thành công", response));
    }

    @GetMapping("/brands")
    @Operation(summary = "Get featured brands for storefront filter")
    public ResponseEntity<ApiResponse<List<BrandResponse>>> getFeaturedBrands() {
        List<BrandResponse> response = storefrontProductService.getFeaturedBrands();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách thương hiệu thành công", response));
    }
}


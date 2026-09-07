package com.techstore.controller;

import com.techstore.dto.response.ApiResponse;
import com.techstore.dto.response.PageResponse;
import com.techstore.dto.response.StorefrontProductDetailResponse;
import com.techstore.dto.response.StorefrontProductResponse;
import com.techstore.service.StorefrontProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("${app.api.base-path}/products")
@Validated
@Tag(name = "Product", description = "Public storefront product endpoints")
public class ProductController {

    private final StorefrontProductService storefrontProductService;

    public ProductController(StorefrontProductService storefrontProductService) {
        this.storefrontProductService = storefrontProductService;
    }

    @GetMapping("/search")
    @Operation(summary = "Search products by keyword (name, description, brand)")
    public ResponseEntity<ApiResponse<List<StorefrontProductResponse>>> searchProducts(
            @RequestParam(name = "q")
            @NotBlank(message = "Từ khóa tìm kiếm không được để trống")
            @Size(max = 100, message = "Từ khóa tìm kiếm tối đa 100 ký tự")
            String q
    ) {
        List<StorefrontProductResponse> response = storefrontProductService.searchProducts(q);
        return ResponseEntity.ok(ApiResponse.success("Tìm kiếm sản phẩm thành công", response));
    }

    @GetMapping
    @Operation(summary = "Get products with optional combined filters, sorting, and pagination")
    public ResponseEntity<ApiResponse<?>> getProducts(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) List<Long> brandIds,
            @RequestParam(required = false) BigDecimal priceMin,
            @RequestParam(required = false) BigDecimal priceMax,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        if (page != null || size != null) {
            int pageNum = page != null ? page : 0;
            int pageSize = size != null ? size : 12;
            PageResponse<StorefrontProductResponse> response = storefrontProductService.getPaginatedProducts(
                    categoryId, brandIds, priceMin, priceMax, sortBy, sortDir, pageNum, pageSize
            );
            return ResponseEntity.ok(ApiResponse.success("Lấy danh sách sản phẩm thành công", response));
        }

        List<StorefrontProductResponse> response = storefrontProductService.getProducts(
                categoryId, brandIds, priceMin, priceMax, sortBy, sortDir
        );
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách sản phẩm thành công", response));
    }

    @GetMapping("/featured")
    @Operation(summary = "Get featured products")
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
    @Operation(summary = "Get new arrival products")
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
    @Operation(summary = "Get on sale products")
    public ResponseEntity<ApiResponse<List<StorefrontProductResponse>>> getOnSale(
            @RequestParam(defaultValue = "8")
            @Min(value = 1, message = "Số lượng sản phẩm tối thiểu là 1")
            @Max(value = 50, message = "Số lượng sản phẩm tối đa là 50")
            int limit
    ) {
        List<StorefrontProductResponse> response = storefrontProductService.getOnSaleProducts(limit);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách sản phẩm khuyến mãi thành công", response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get storefront product details by ID")
    public ResponseEntity<ApiResponse<StorefrontProductDetailResponse>> getProductDetail(
            @PathVariable
            @Positive(message = "ID sản phẩm không hợp lệ")
            Long id
    ) {
        StorefrontProductDetailResponse response = storefrontProductService.getProductDetail(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin chi tiết sản phẩm thành công", response));
    }
}


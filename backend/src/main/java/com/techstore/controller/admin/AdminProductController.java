package com.techstore.controller.admin;

import com.techstore.dto.request.ProductCreateRequest;
import com.techstore.dto.request.ProductVariantRequest;
import com.techstore.dto.response.ApiResponse;
import com.techstore.dto.response.ProductResponse;
import com.techstore.dto.response.ProductVariantResponse;
import com.techstore.enums.RoleCode;
import com.techstore.security.RequireRole;
import com.techstore.service.ProductService;
import com.techstore.service.ProductVariantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("${app.api.base-path}/admin/products")
@RequireRole(RoleCode.ADMIN)
@Tag(name = "Admin Product Management", description = "Admin product management endpoints")
public class AdminProductController {

    private final ProductService productService;
    private final ProductVariantService productVariantService;

    public AdminProductController(
            ProductService productService,
            ProductVariantService productVariantService
    ) {
        this.productService = productService;
        this.productVariantService = productVariantService;
    }

    @PostMapping
    @Operation(summary = "Create a new product with basic information")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @Valid @RequestBody ProductCreateRequest request
    ) {
        ProductResponse response = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo sản phẩm thành công", response));
    }

    @GetMapping
    @Operation(summary = "Get list of all products for admin")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getAllProducts() {
        List<ProductResponse> response = productService.getAllAdminProducts();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách sản phẩm thành công", response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product details by ID")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(@PathVariable("id") Long id) {
        ProductResponse response = productService.getProductById(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin sản phẩm thành công", response));
    }

    // --- Product Variant Endpoints ---

    @PostMapping("/{id}/variants")
    @Operation(summary = "Create a new variant for a product")
    public ResponseEntity<ApiResponse<ProductVariantResponse>> createVariant(
            @PathVariable("id") Long productId,
            @Valid @RequestBody ProductVariantRequest request
    ) {
        ProductVariantResponse response = productVariantService.createVariant(productId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo biến thể sản phẩm thành công", response));
    }

    @GetMapping("/{id}/variants")
    @Operation(summary = "Get all variants of a product")
    public ResponseEntity<ApiResponse<List<ProductVariantResponse>>> getVariants(
            @PathVariable("id") Long productId
    ) {
        List<ProductVariantResponse> response = productVariantService.getVariantsByProductId(productId);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách biến thể thành công", response));
    }

    @GetMapping("/{id}/variants/{variantId}")
    @Operation(summary = "Get variant details by ID")
    public ResponseEntity<ApiResponse<ProductVariantResponse>> getVariantById(
            @PathVariable("id") Long productId,
            @PathVariable("variantId") Long variantId
    ) {
        ProductVariantResponse response = productVariantService.getVariantById(productId, variantId);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin biến thể thành công", response));
    }

    @PutMapping("/{id}/variants/{variantId}")
    @Operation(summary = "Update a product variant")
    public ResponseEntity<ApiResponse<ProductVariantResponse>> updateVariant(
            @PathVariable("id") Long productId,
            @PathVariable("variantId") Long variantId,
            @Valid @RequestBody ProductVariantRequest request
    ) {
        ProductVariantResponse response = productVariantService.updateVariant(productId, variantId, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật biến thể sản phẩm thành công", response));
    }

    @DeleteMapping("/{id}/variants/{variantId}")
    @Operation(summary = "Delete a product variant")
    public ResponseEntity<ApiResponse<Void>> deleteVariant(
            @PathVariable("id") Long productId,
            @PathVariable("variantId") Long variantId
    ) {
        productVariantService.deleteVariant(productId, variantId);
        return ResponseEntity.ok(ApiResponse.success("Xoá biến thể sản phẩm thành công", null));
    }
}

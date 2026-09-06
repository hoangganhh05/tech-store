package com.techstore.controller.admin;

import com.techstore.dto.request.ProductCreateRequest;
import com.techstore.dto.request.ProductImageUpdateRequest;
import com.techstore.dto.request.ProductSpecificationRequest;
import com.techstore.dto.request.ProductStatusUpdateRequest;
import com.techstore.dto.request.ProductUpdateRequest;
import com.techstore.dto.request.ProductVariantRequest;
import com.techstore.dto.response.ApiResponse;
import com.techstore.dto.response.ProductImageResponse;
import com.techstore.dto.response.ProductResponse;
import com.techstore.dto.response.ProductSpecificationResponse;
import com.techstore.dto.response.ProductVariantResponse;
import com.techstore.enums.RoleCode;
import com.techstore.security.RequireRole;
import com.techstore.service.ProductImageService;
import com.techstore.service.ProductService;
import com.techstore.service.ProductSpecificationService;
import com.techstore.service.ProductVariantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("${app.api.base-path}/admin/products")
@RequireRole(RoleCode.ADMIN)
@Tag(name = "Admin Product Management", description = "Admin product management endpoints")
public class AdminProductController {

    private final ProductService productService;
    private final ProductVariantService productVariantService;
    private final ProductImageService productImageService;
    private final ProductSpecificationService productSpecificationService;

    public AdminProductController(
            ProductService productService,
            ProductVariantService productVariantService,
            ProductImageService productImageService,
            ProductSpecificationService productSpecificationService
    ) {
        this.productService = productService;
        this.productVariantService = productVariantService;
        this.productImageService = productImageService;
        this.productSpecificationService = productSpecificationService;
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

    @PutMapping("/{id}")
    @Operation(summary = "Update product basic information")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable("id") Long id,
            @Valid @RequestBody ProductUpdateRequest request
    ) {
        ProductResponse response = productService.updateProduct(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật sản phẩm thành công", response));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update product status (DRAFT, ACTIVE, INACTIVE)")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProductStatus(
            @PathVariable("id") Long id,
            @Valid @RequestBody ProductStatusUpdateRequest request
    ) {
        ProductResponse response = productService.updateProductStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái sản phẩm thành công", response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft delete a product")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable("id") Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success("Xoá sản phẩm thành công", null));
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

    // --- Product Image Endpoints ---

    @PostMapping(value = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload an image for a product")
    public ResponseEntity<ApiResponse<ProductImageResponse>> uploadImage(
            @PathVariable("id") Long productId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "variantId", required = false) Long variantId,
            @RequestParam(value = "isPrimary", required = false, defaultValue = "false") Boolean isPrimary
    ) {
        ProductImageResponse response = productImageService.uploadImage(productId, file, variantId, isPrimary);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tải lên hình ảnh thành công", response));
    }

    @GetMapping("/{id}/images")
    @Operation(summary = "Get all images of a product")
    public ResponseEntity<ApiResponse<List<ProductImageResponse>>> getImages(
            @PathVariable("id") Long productId
    ) {
        List<ProductImageResponse> response = productImageService.getImagesByProductId(productId);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách hình ảnh thành công", response));
    }

    @PutMapping("/{id}/images/{imageId}/primary")
    @Operation(summary = "Set an image as primary for a product")
    public ResponseEntity<ApiResponse<ProductImageResponse>> setPrimaryImage(
            @PathVariable("id") Long productId,
            @PathVariable("imageId") Long imageId
    ) {
        ProductImageResponse response = productImageService.setPrimaryImage(productId, imageId);
        return ResponseEntity.ok(ApiResponse.success("Đặt ảnh đại diện thành công", response));
    }

    @PutMapping("/{id}/images/{imageId}")
    @Operation(summary = "Update product image info (variant, display order)")
    public ResponseEntity<ApiResponse<ProductImageResponse>> updateImage(
            @PathVariable("id") Long productId,
            @PathVariable("imageId") Long imageId,
            @RequestBody ProductImageUpdateRequest request
    ) {
        ProductImageResponse response = productImageService.updateImage(productId, imageId, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật hình ảnh thành công", response));
    }

    @DeleteMapping("/{id}/images/{imageId}")
    @Operation(summary = "Delete an image of a product")
    public ResponseEntity<ApiResponse<Void>> deleteImage(
            @PathVariable("id") Long productId,
            @PathVariable("imageId") Long imageId
    ) {
        productImageService.deleteImage(productId, imageId);
        return ResponseEntity.ok(ApiResponse.success("Xoá hình ảnh thành công", null));
    }

    // --- Product Specification Endpoints ---

    @PostMapping("/{id}/specifications")
    @Operation(summary = "Create a new specification for a product")
    public ResponseEntity<ApiResponse<ProductSpecificationResponse>> createSpecification(
            @PathVariable("id") Long productId,
            @Valid @RequestBody ProductSpecificationRequest request
    ) {
        ProductSpecificationResponse response = productSpecificationService.createSpecification(productId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo thông số kỹ thuật thành công", response));
    }

    @GetMapping("/{id}/specifications")
    @Operation(summary = "Get all specifications of a product")
    public ResponseEntity<ApiResponse<List<ProductSpecificationResponse>>> getSpecifications(
            @PathVariable("id") Long productId
    ) {
        List<ProductSpecificationResponse> response = productSpecificationService.getSpecifications(productId);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách thông số kỹ thuật thành công", response));
    }

    @GetMapping("/{id}/specifications/{specId}")
    @Operation(summary = "Get specification details by ID")
    public ResponseEntity<ApiResponse<ProductSpecificationResponse>> getSpecificationById(
            @PathVariable("id") Long productId,
            @PathVariable("specId") Long specId
    ) {
        ProductSpecificationResponse response = productSpecificationService.getSpecificationById(productId, specId);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin thông số kỹ thuật thành công", response));
    }

    @PutMapping("/{id}/specifications/{specId}")
    @Operation(summary = "Update a product specification")
    public ResponseEntity<ApiResponse<ProductSpecificationResponse>> updateSpecification(
            @PathVariable("id") Long productId,
            @PathVariable("specId") Long specId,
            @Valid @RequestBody ProductSpecificationRequest request
    ) {
        ProductSpecificationResponse response = productSpecificationService.updateSpecification(productId, specId, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật thông số kỹ thuật thành công", response));
    }

    @DeleteMapping("/{id}/specifications/{specId}")
    @Operation(summary = "Delete a product specification")
    public ResponseEntity<ApiResponse<Void>> deleteSpecification(
            @PathVariable("id") Long productId,
            @PathVariable("specId") Long specId
    ) {
        productSpecificationService.deleteSpecification(productId, specId);
        return ResponseEntity.ok(ApiResponse.success("Xoá thông số kỹ thuật thành công", null));
    }
}

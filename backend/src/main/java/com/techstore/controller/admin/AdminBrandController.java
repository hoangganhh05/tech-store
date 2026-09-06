package com.techstore.controller.admin;

import com.techstore.dto.request.BrandRequest;
import com.techstore.dto.response.ApiResponse;
import com.techstore.dto.response.BrandResponse;
import com.techstore.enums.RoleCode;
import com.techstore.security.RequireRole;
import com.techstore.service.BrandService;
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
@RequestMapping("${app.api.base-path}/admin/brands")
@RequireRole(RoleCode.ADMIN)
@Tag(name = "Admin Brand Management", description = "Admin brand management endpoints")
public class AdminBrandController {

    private final BrandService brandService;

    public AdminBrandController(BrandService brandService) {
        this.brandService = brandService;
    }

    @PostMapping
    @Operation(summary = "Create a new brand")
    public ResponseEntity<ApiResponse<BrandResponse>> createBrand(
            @Valid @RequestBody BrandRequest request
    ) {
        BrandResponse response = brandService.createBrand(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo thương hiệu thành công", response));
    }

    @GetMapping
    @Operation(summary = "Get list of all brands")
    public ResponseEntity<ApiResponse<List<BrandResponse>>> getAllBrands() {
        List<BrandResponse> response = brandService.getAllBrands();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách thương hiệu thành công", response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get brand details by ID")
    public ResponseEntity<ApiResponse<BrandResponse>> getBrandById(@PathVariable("id") Long id) {
        BrandResponse response = brandService.getBrandById(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết thương hiệu thành công", response));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing brand")
    public ResponseEntity<ApiResponse<BrandResponse>> updateBrand(
            @PathVariable("id") Long id,
            @Valid @RequestBody BrandRequest request
    ) {
        BrandResponse response = brandService.updateBrand(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật thương hiệu thành công", response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a brand")
    public ResponseEntity<ApiResponse<Void>> deleteBrand(@PathVariable("id") Long id) {
        brandService.deleteBrand(id);
        return ResponseEntity.ok(ApiResponse.success("Xoá thương hiệu thành công", null));
    }
}

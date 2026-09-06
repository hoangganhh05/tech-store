package com.techstore.controller.admin;

import com.techstore.dto.response.ApiResponse;
import com.techstore.dto.response.InventoryResponse;
import com.techstore.dto.response.InventorySummaryResponse;
import com.techstore.dto.response.PageResponse;
import com.techstore.enums.RoleCode;
import com.techstore.enums.StockStatus;
import com.techstore.security.RequireRole;
import com.techstore.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${app.api.base-path}/admin/inventory")
@RequireRole(RoleCode.ADMIN)
@Tag(name = "Admin Inventory", description = "Quản lý tồn kho sản phẩm cho Admin")
public class AdminInventoryController {

    private final InventoryService inventoryService;

    public AdminInventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    @Operation(summary = "Xem danh sách tồn kho phân trang có tìm kiếm và lọc")
    public ApiResponse<PageResponse<InventoryResponse>> getInventories(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "ALL") StockStatus stockStatus,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        if (page < 0) {
            page = 0;
        }
        if (size < 1) {
            size = 10;
        } else if (size > 100) {
            size = 100;
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        PageResponse<InventoryResponse> response = inventoryService.getInventories(search, categoryId, stockStatus, pageable);
        return ApiResponse.success("Lấy danh sách tồn kho thành công", response);
    }

    @GetMapping("/summary")
    @Operation(summary = "Lấy thống kê tổng quan tồn kho")
    public ApiResponse<InventorySummaryResponse> getSummary() {
        InventorySummaryResponse summary = inventoryService.getInventorySummary();
        return ApiResponse.success("Lấy thống kê tồn kho thành công", summary);
    }

    @GetMapping("/variants/{variantId}")
    @Operation(summary = "Xem chi tiết tồn kho của một biến thể sản phẩm")
    public ApiResponse<InventoryResponse> getByVariantId(@PathVariable Long variantId) {
        InventoryResponse response = inventoryService.getInventoryByVariantId(variantId);
        return ApiResponse.success("Lấy thông tin tồn kho thành công", response);
    }
}

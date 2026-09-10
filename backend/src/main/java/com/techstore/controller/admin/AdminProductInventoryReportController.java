package com.techstore.controller.admin;

import com.techstore.dto.response.ApiResponse;
import com.techstore.dto.response.ProductInventoryReportResponse;
import com.techstore.enums.RoleCode;
import com.techstore.security.RequireRole;
import com.techstore.service.ProductInventoryReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("${app.api.base-path}/admin/reports/products")
@RequireRole(RoleCode.ADMIN)
@Tag(name = "Admin Product Reports", description = "Báo cáo sản phẩm bán chạy và tồn kho thấp")
public class AdminProductInventoryReportController {

    private final ProductInventoryReportService reportService;

    public AdminProductInventoryReportController(ProductInventoryReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping
    @Operation(summary = "Xem top sản phẩm bán chạy và danh sách tồn kho thấp")
    public ApiResponse<ProductInventoryReportResponse> getReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "QUANTITY") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection
    ) {
        return ApiResponse.success("Lấy báo cáo sản phẩm thành công",
                reportService.getReport(fromDate, toDate, categoryId, sortBy, sortDirection));
    }
}

package com.techstore.controller.admin;

import com.techstore.dto.response.ApiResponse;
import com.techstore.dto.response.RevenueReportResponse;
import com.techstore.enums.RoleCode;
import com.techstore.security.RequireRole;
import com.techstore.service.RevenueReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("${app.api.base-path}/admin/reports/revenue")
@RequireRole(RoleCode.ADMIN)
@Tag(name = "Admin Revenue Reports", description = "Báo cáo doanh thu theo khoảng thời gian")
public class AdminRevenueReportController {

    private final RevenueReportService revenueReportService;

    public AdminRevenueReportController(RevenueReportService revenueReportService) {
        this.revenueReportService = revenueReportService;
    }

    @GetMapping
    @Operation(summary = "Xem báo cáo doanh thu chi tiết theo khoảng ngày tùy chọn")
    public ApiResponse<RevenueReportResponse> getRevenueReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        return ApiResponse.success("Lấy báo cáo doanh thu thành công",
                revenueReportService.getRevenueReport(fromDate, toDate));
    }
}

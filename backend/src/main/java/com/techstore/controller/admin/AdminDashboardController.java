package com.techstore.controller.admin;

import com.techstore.dto.response.AdminDashboardResponse;
import com.techstore.dto.response.ApiResponse;
import com.techstore.enums.RoleCode;
import com.techstore.security.RequireRole;
import com.techstore.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("${app.api.base-path}/admin/dashboard")
@RequireRole(RoleCode.ADMIN)
@Tag(name = "Admin Dashboard", description = "Thống kê tổng quan hoạt động kinh doanh")
public class AdminDashboardController {

    private final DashboardService dashboardService;

    public AdminDashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    @Operation(summary = "Xem dashboard doanh thu, đơn hàng và sản phẩm bán chạy theo ngày hoặc tháng")
    public ApiResponse<AdminDashboardResponse> getDashboard(
            @RequestParam(defaultValue = "MONTH") String period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ApiResponse.success("Lấy thống kê dashboard thành công",
                dashboardService.getAdminDashboard(period, date));
    }
}

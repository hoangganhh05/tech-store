package com.techstore.controller.admin;

import com.techstore.dto.response.AdminOrderDetailResponse;
import com.techstore.dto.response.AdminOrderSummaryResponse;
import com.techstore.dto.response.ApiResponse;
import com.techstore.dto.response.PageResponse;
import com.techstore.enums.RoleCode;
import com.techstore.security.RequireRole;
import com.techstore.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("${app.api.base-path}/admin/orders")
@RequireRole(RoleCode.ADMIN)
@Validated
@Tag(name = "Admin Orders", description = "Quản lý danh sách đơn hàng")
public class AdminOrderController {

    private final OrderService orderService;

    public AdminOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    @Operation(summary = "Xem danh sách đơn hàng có tìm kiếm, lọc và phân trang")
    public ApiResponse<PageResponse<AdminOrderSummaryResponse>> getOrders(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.success("Lấy danh sách đơn hàng thành công",
                orderService.getAdminOrders(search, status, fromDate, toDate, page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Xem đầy đủ chi tiết một đơn hàng")
    public ApiResponse<AdminOrderDetailResponse> getOrderDetail(
            @PathVariable @Positive(message = "Mã đơn hàng không hợp lệ") Long id
    ) {
        return ApiResponse.success("Lấy chi tiết đơn hàng thành công", orderService.getAdminOrderDetail(id));
    }
}

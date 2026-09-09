package com.techstore.controller;

import com.techstore.dto.request.PlaceOrderRequest;
import com.techstore.dto.request.CancelOrderRequest;
import com.techstore.dto.response.ApiResponse;
import com.techstore.dto.response.OrderHistoryResponse;
import com.techstore.dto.response.OrderDetailResponse;
import com.techstore.dto.response.OrderCancellationResponse;
import com.techstore.dto.response.PageResponse;
import com.techstore.dto.response.PlacedOrderResponse;
import com.techstore.security.RequireRole;
import com.techstore.enums.RoleCode;
import com.techstore.security.RoleAuthorizationInterceptor;
import com.techstore.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${app.api.base-path}/orders")
@Tag(name = "Orders", description = "Customer order placement")
public class OrderController {
    private final OrderService service;
    public OrderController(OrderService service) { this.service = service; }
    @PostMapping
    @RequireRole(RoleCode.CUSTOMER)
    @Operation(summary = "Tạo đơn hàng và trừ tồn kho trong một transaction")
    public ApiResponse<PlacedOrderResponse> place(@RequestAttribute(RoleAuthorizationInterceptor.CURRENT_USER_ID_ATTRIBUTE) Long userId,
                                                   @Valid @RequestBody PlaceOrderRequest request) {
        return ApiResponse.success("Đặt hàng thành công", service.placeOrder(userId, request));
    }

    @GetMapping("/my-orders")
    @RequireRole(RoleCode.CUSTOMER)
    @Operation(summary = "Xem lịch sử đơn hàng của khách hàng hiện tại")
    public ApiResponse<PageResponse<OrderHistoryResponse>> getMyOrders(
            @RequestAttribute(RoleAuthorizationInterceptor.CURRENT_USER_ID_ATTRIBUTE) Long userId,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        return ApiResponse.success("Lấy lịch sử đơn hàng thành công", service.getMyOrders(userId, status, page, size));
    }

    @GetMapping("/{id}")
    @RequireRole({RoleCode.CUSTOMER, RoleCode.ADMIN})
    @Operation(summary = "Xem chi tiết đơn hàng và lịch sử trạng thái")
    public ApiResponse<OrderDetailResponse> getOrderDetail(
            @RequestAttribute(RoleAuthorizationInterceptor.CURRENT_USER_ID_ATTRIBUTE) Long userId,
            @PathVariable Long id
    ) {
        return ApiResponse.success("Lấy chi tiết đơn hàng thành công", service.getOrderDetail(userId, id));
    }

    @PatchMapping("/{id}/cancel")
    @RequireRole(RoleCode.CUSTOMER)
    @Operation(summary = "Khách hàng huỷ đơn hàng khi đơn chưa giao")
    public ApiResponse<OrderCancellationResponse> cancelOrder(
            @RequestAttribute(RoleAuthorizationInterceptor.CURRENT_USER_ID_ATTRIBUTE) Long userId,
            @PathVariable Long id,
            @Valid @RequestBody(required = false) CancelOrderRequest request
    ) {
        return ApiResponse.success("Huỷ đơn hàng thành công", service.cancelOrder(userId, id, request));
    }
}

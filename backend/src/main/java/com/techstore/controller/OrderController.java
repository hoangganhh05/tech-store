package com.techstore.controller;

import com.techstore.dto.request.PlaceOrderRequest;
import com.techstore.dto.response.ApiResponse;
import com.techstore.dto.response.OrderHistoryResponse;
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
@RequireRole(RoleCode.CUSTOMER)
@Tag(name = "Orders", description = "Customer order placement")
public class OrderController {
    private final OrderService service;
    public OrderController(OrderService service) { this.service = service; }
    @PostMapping
    @Operation(summary = "Tạo đơn hàng và trừ tồn kho trong một transaction")
    public ApiResponse<PlacedOrderResponse> place(@RequestAttribute(RoleAuthorizationInterceptor.CURRENT_USER_ID_ATTRIBUTE) Long userId,
                                                   @Valid @RequestBody PlaceOrderRequest request) {
        return ApiResponse.success("Đặt hàng thành công", service.placeOrder(userId, request));
    }

    @GetMapping("/my-orders")
    @Operation(summary = "Xem lịch sử đơn hàng của khách hàng hiện tại")
    public ApiResponse<PageResponse<OrderHistoryResponse>> getMyOrders(
            @RequestAttribute(RoleAuthorizationInterceptor.CURRENT_USER_ID_ATTRIBUTE) Long userId,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        return ApiResponse.success("Lấy lịch sử đơn hàng thành công", service.getMyOrders(userId, status, page, size));
    }
}

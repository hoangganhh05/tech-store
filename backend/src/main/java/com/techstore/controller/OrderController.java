package com.techstore.controller;

import com.techstore.dto.request.PlaceOrderRequest;
import com.techstore.dto.response.ApiResponse;
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
}

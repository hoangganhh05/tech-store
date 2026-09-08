package com.techstore.controller;

import com.techstore.dto.request.SelectPaymentMethodRequest;
import com.techstore.dto.response.ApiResponse;
import com.techstore.dto.response.PaymentMethodResponse;
import com.techstore.enums.RoleCode;
import com.techstore.security.RequireRole;
import com.techstore.service.PaymentMethodService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("${app.api.base-path}/checkout")
@RequireRole(RoleCode.CUSTOMER)
@Tag(name = "Checkout payment", description = "Payment method selection; online payments are simulated")
public class CheckoutPaymentController {
    private final PaymentMethodService service;

    public CheckoutPaymentController(PaymentMethodService service) { this.service = service; }

    @GetMapping("/payment-methods")
    @Operation(summary = "Danh sách phương thức thanh toán và hướng dẫn")
    public ApiResponse<List<PaymentMethodResponse>> getMethods() {
        return ApiResponse.success("Danh sách phương thức thanh toán", service.getMethods());
    }

    @PostMapping("/payment-method")
    @Operation(summary = "Kiểm tra lựa chọn thanh toán", description = "Trả lựa chọn hợp lệ cho checkout. Không tạo đơn hoặc thu tiền; phương thức được ghi vào Order khi tạo đơn.")
    public ApiResponse<PaymentMethodResponse> select(@Valid @RequestBody SelectPaymentMethodRequest request) {
        return ApiResponse.success("Đã chọn phương thức thanh toán", service.select(request.paymentMethod()));
    }
}

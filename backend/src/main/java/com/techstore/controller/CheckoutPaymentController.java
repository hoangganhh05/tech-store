package com.techstore.controller;

import com.techstore.dto.request.SelectPaymentMethodRequest;
import com.techstore.dto.request.CheckoutReviewRequest;
import com.techstore.dto.request.ApplyVoucherRequest;
import com.techstore.dto.response.ApiResponse;
import com.techstore.dto.response.CheckoutReviewResponse;
import com.techstore.dto.response.PaymentMethodResponse;
import com.techstore.dto.response.VoucherApplicationResponse;
import com.techstore.enums.RoleCode;
import com.techstore.security.RequireRole;
import com.techstore.service.PaymentMethodService;
import com.techstore.service.CheckoutReviewService;
import com.techstore.service.VoucherService;
import com.techstore.security.RoleAuthorizationInterceptor;
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
    private final CheckoutReviewService checkoutReviewService;
    private final VoucherService voucherService;

    public CheckoutPaymentController(PaymentMethodService service, CheckoutReviewService checkoutReviewService, VoucherService voucherService) {
        this.service = service;
        this.checkoutReviewService = checkoutReviewService;
        this.voucherService = voucherService;
    }

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

    @PostMapping("/review")
    @Operation(summary = "Tổng hợp và kiểm tra dữ liệu đơn hàng trước khi đặt")
    public ApiResponse<CheckoutReviewResponse> review(
            @RequestAttribute(RoleAuthorizationInterceptor.CURRENT_USER_ID_ATTRIBUTE) Long userId,
            @Valid @RequestBody CheckoutReviewRequest request) {
        return ApiResponse.success("Thông tin xem lại đơn hàng", checkoutReviewService.review(userId, request));
    }

    @PostMapping("/voucher")
    @Operation(summary = "Kiểm tra và áp dụng mã voucher cho giỏ hàng")
    public ApiResponse<VoucherApplicationResponse> applyVoucher(
            @RequestAttribute(RoleAuthorizationInterceptor.CURRENT_USER_ID_ATTRIBUTE) Long userId,
            @Valid @RequestBody ApplyVoucherRequest request) {
        return ApiResponse.success("Áp dụng voucher thành công", voucherService.apply(userId, request.code()));
    }
}

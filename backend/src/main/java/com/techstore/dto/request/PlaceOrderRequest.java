package com.techstore.dto.request;

import com.techstore.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PlaceOrderRequest(
        @NotNull(message = "Vui lòng chọn địa chỉ giao hàng") @Positive(message = "Mã địa chỉ không hợp lệ") Long addressId,
        @NotNull(message = "Vui lòng chọn phương thức thanh toán") PaymentMethod paymentMethod
) {}

package com.techstore.dto.request;

import com.techstore.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;

public record SelectPaymentMethodRequest(
        @NotNull(message = "Vui lòng chọn phương thức thanh toán") PaymentMethod paymentMethod
) {}

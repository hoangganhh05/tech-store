package com.techstore.dto.request;

import com.techstore.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CheckoutReviewRequest(
        @NotNull(message = "Vui lòng chọn địa chỉ giao hàng")
        @Positive(message = "Mã địa chỉ giao hàng không hợp lệ") Long addressId,
        @NotNull(message = "Vui lòng chọn phương thức thanh toán") PaymentMethod paymentMethod
        , @Size(max = 50, message = "Mã voucher không được vượt quá 50 ký tự") String voucherCode
) {
    public CheckoutReviewRequest(Long addressId, PaymentMethod paymentMethod) {
        this(addressId, paymentMethod, null);
    }
}

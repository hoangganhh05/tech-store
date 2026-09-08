package com.techstore.dto.response;

import com.techstore.enums.DiscountType;
import java.math.BigDecimal;

public record VoucherApplicationResponse(
        String code,
        String name,
        DiscountType discountType,
        BigDecimal discountAmount,
        BigDecimal subtotal,
        BigDecimal shippingFee,
        BigDecimal total
) {}

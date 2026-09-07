package com.techstore.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record CartResponse(
        Long id,
        Integer totalItems,
        BigDecimal subtotal,
        BigDecimal shippingFee,
        BigDecimal discountAmount,
        BigDecimal total,
        boolean hasStockIssue,
        boolean canCheckout,
        List<CartItemResponse> items
) {
    public CartResponse(
            Long id,
            Integer totalItems,
            BigDecimal subtotal,
            BigDecimal shippingFee,
            BigDecimal discountAmount,
            BigDecimal total,
            List<CartItemResponse> items
    ) {
        this(id, totalItems, subtotal, shippingFee, discountAmount, total, false, totalItems != null && totalItems > 0, items);
    }

    public CartResponse(Long id, Integer totalItems, BigDecimal subtotal, List<CartItemResponse> items) {
        this(id, totalItems, subtotal, BigDecimal.ZERO, BigDecimal.ZERO, subtotal != null ? subtotal : BigDecimal.ZERO, false, totalItems != null && totalItems > 0, items);
    }
}

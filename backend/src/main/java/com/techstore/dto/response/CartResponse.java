package com.techstore.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record CartResponse(
        Long id,
        Integer totalItems,
        BigDecimal subtotal,
        List<CartItemResponse> items
) {
}


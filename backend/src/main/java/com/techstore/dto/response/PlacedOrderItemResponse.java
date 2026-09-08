package com.techstore.dto.response;

import java.math.BigDecimal;

public record PlacedOrderItemResponse(
        String productName,
        String variantLabel,
        BigDecimal unitPrice,
        Integer quantity,
        BigDecimal subtotal
) {}

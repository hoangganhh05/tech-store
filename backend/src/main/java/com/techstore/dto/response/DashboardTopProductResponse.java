package com.techstore.dto.response;

import java.math.BigDecimal;

public record DashboardTopProductResponse(
        String productName,
        long quantitySold,
        BigDecimal revenue
) {
}

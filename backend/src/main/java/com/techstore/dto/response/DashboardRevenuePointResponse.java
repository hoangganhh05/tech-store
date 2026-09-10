package com.techstore.dto.response;

import java.math.BigDecimal;

public record DashboardRevenuePointResponse(
        String label,
        BigDecimal revenue,
        long orderCount
) {
}

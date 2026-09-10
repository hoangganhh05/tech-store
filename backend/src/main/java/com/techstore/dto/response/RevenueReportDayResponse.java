package com.techstore.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RevenueReportDayResponse(
        LocalDate date,
        BigDecimal revenue,
        long orderCount,
        BigDecimal averageOrderValue
) {
}

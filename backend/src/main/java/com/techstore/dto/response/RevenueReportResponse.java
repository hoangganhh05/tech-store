package com.techstore.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record RevenueReportResponse(
        LocalDate fromDate,
        LocalDate toDate,
        BigDecimal totalRevenue,
        long totalOrders,
        BigDecimal averageOrderValue,
        List<RevenueReportDayResponse> dailyRevenue
) {
}

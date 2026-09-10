package com.techstore.dto.response;

import com.techstore.enums.DashboardPeriod;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record AdminDashboardResponse(
        DashboardPeriod period,
        LocalDate fromDate,
        LocalDate toDate,
        BigDecimal totalRevenue,
        long totalOrders,
        List<DashboardStatusCountResponse> ordersByStatus,
        List<DashboardTopProductResponse> topSellingProducts,
        List<DashboardRevenuePointResponse> revenueTrend
) {
}

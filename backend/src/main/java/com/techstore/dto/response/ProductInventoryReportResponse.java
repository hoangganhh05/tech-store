package com.techstore.dto.response;

import java.time.LocalDate;
import java.util.List;

public record ProductInventoryReportResponse(
        LocalDate fromDate,
        LocalDate toDate,
        Long categoryId,
        String sortBy,
        String sortDirection,
        List<ProductSalesReportItem> topSellingProducts,
        List<LowStockReportItem> lowStockVariants
) {
}

package com.techstore.dto.response;

import java.math.BigDecimal;

public record ProductSalesReportItem(
        String productName,
        Long categoryId,
        String categoryName,
        long quantitySold,
        BigDecimal revenue
) {
}

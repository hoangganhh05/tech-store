package com.techstore.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record OrderInventoryDeductionRequest(
        Long orderId,
        String orderCode,

        @NotEmpty(message = "Danh sách sản phẩm trong đơn không được để trống")
        @Valid
        List<OrderItemStockRequest> items,

        String note
) {
}

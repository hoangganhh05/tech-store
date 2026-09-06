package com.techstore.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record OrderInventoryRestoreRequest(
        Long orderId,
        String orderCode,

        @NotEmpty(message = "Danh sách sản phẩm hoàn kho không được để trống")
        @Valid
        List<OrderItemStockRequest> items,

        @NotBlank(message = "Lý do hoàn tồn kho không được để trống")
        String reason
) {
}

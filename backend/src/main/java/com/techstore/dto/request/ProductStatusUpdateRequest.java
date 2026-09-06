package com.techstore.dto.request;

import com.techstore.enums.ProductStatus;
import jakarta.validation.constraints.NotNull;

public record ProductStatusUpdateRequest(
        @NotNull(message = "Trạng thái sản phẩm không được để trống")
        ProductStatus status
) {
}

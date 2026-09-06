package com.techstore.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateThresholdRequest(
        @NotNull(message = "Ngưỡng tồn kho không được để trống")
        @Min(value = 0, message = "Ngưỡng tồn kho phải lớn hơn hoặc bằng 0")
        Integer lowStockThreshold
) {
}
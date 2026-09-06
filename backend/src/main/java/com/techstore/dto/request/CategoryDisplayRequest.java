package com.techstore.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CategoryDisplayRequest(
        @NotNull(message = "Thứ tự hiển thị không được để trống")
        @Min(value = 0, message = "Thứ tự hiển thị phải >= 0")
        Integer displayOrder,

        @NotNull(message = "Trạng thái hiển thị không được để trống")
        Boolean isActive
) {
}

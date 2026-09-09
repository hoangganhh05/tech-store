package com.techstore.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateOrderStatusRequest(
        @NotBlank(message = "Trạng thái đơn hàng không được để trống")
        @Size(max = 25, message = "Trạng thái đơn hàng không được vượt quá 25 ký tự")
        String status
) {
}

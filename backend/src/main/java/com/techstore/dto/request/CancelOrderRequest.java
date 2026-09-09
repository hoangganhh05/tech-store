package com.techstore.dto.request;

import jakarta.validation.constraints.Size;

public record CancelOrderRequest(
        @Size(max = 500, message = "Lý do huỷ không được vượt quá 500 ký tự")
        String reason
) {
}

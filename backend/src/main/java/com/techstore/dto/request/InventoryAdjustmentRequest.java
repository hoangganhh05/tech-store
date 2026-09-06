package com.techstore.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record InventoryAdjustmentRequest(
        @NotNull(message = "ID biến thể không được để trống")
        Long variantId,

        @NotNull(message = "Số lượng điều chỉnh không được để trống")
        Integer quantityChange,

        @NotBlank(message = "Lý do điều chỉnh không được để trống")
        @Size(max = 500, message = "Lý do điều chỉnh không được vượt quá 500 ký tự")
        String reason,

        String referenceType,
        Long referenceId
) {
}
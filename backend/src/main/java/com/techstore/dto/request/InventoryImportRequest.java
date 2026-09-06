package com.techstore.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record InventoryImportRequest(
        @NotNull(message = "Variant ID không được để trống")
        Long variantId,

        @NotNull(message = "Số lượng nhập không được để trống")
        @Min(value = 1, message = "Số lượng nhập phải lớn hơn 0")
        Integer quantity,

        @Size(max = 500, message = "Ghi chú không vượt quá 500 ký tự")
        String note,

        String referenceType,

        Long referenceId
) {
}
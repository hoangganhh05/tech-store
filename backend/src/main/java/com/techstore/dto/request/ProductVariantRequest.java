package com.techstore.dto.request;

import com.techstore.enums.VariantStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProductVariantRequest(
        @NotBlank(message = "Mã SKU không được để trống")
        @Size(max = 100, message = "Mã SKU không vượt quá 100 ký tự")
        String sku,

        @Size(max = 50, message = "Màu sắc không vượt quá 50 ký tự")
        String color,

        @Size(max = 50, message = "Dung lượng không vượt quá 50 ký tự")
        String storage,

        @NotNull(message = "Giá bán không được để trống")
        @DecimalMin(value = "0.0", inclusive = true, message = "Giá bán phải lớn hơn hoặc bằng 0")
        BigDecimal price,

        @DecimalMin(value = "0.0", inclusive = true, message = "Giá gốc phải lớn hơn hoặc bằng 0")
        BigDecimal originalPrice,

        Integer stockQuantity,

        VariantStatus status
) {
}

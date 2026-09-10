package com.techstore.dto.request;

import com.techstore.enums.PromotionTargetType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

public record PromotionRequest(
        @NotBlank(message = "Tên chương trình không được để trống")
        @Size(max = 150, message = "Tên chương trình không được vượt quá 150 ký tự") String name,
        @NotNull(message = "Phạm vi áp dụng không được để trống") PromotionTargetType targetType,
        @Positive(message = "Mã sản phẩm không hợp lệ") Long productId,
        @Positive(message = "Mã biến thể không hợp lệ") Long variantId,
        @Positive(message = "Mã danh mục không hợp lệ") Long categoryId,
        @NotNull(message = "Mức giảm không được để trống")
        @DecimalMin(value = "0.01", message = "Mức giảm phải lớn hơn 0")
        @DecimalMax(value = "100", message = "Mức giảm không được vượt quá 100%") BigDecimal discountPercent,
        @NotNull(message = "Thời gian bắt đầu không được để trống") Instant startsAt,
        @NotNull(message = "Thời gian kết thúc không được để trống") Instant endsAt,
        Boolean active
) {
}

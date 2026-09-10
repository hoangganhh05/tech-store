package com.techstore.dto.request;

import com.techstore.enums.DiscountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

public record VoucherRequest(
        @NotBlank(message = "Mã voucher không được để trống")
        @Size(max = 50, message = "Mã voucher không được vượt quá 50 ký tự") String code,
        @NotBlank(message = "Tên voucher không được để trống")
        @Size(max = 150, message = "Tên voucher không được vượt quá 150 ký tự") String name,
        @NotNull(message = "Loại giảm giá không được để trống") DiscountType discountType,
        @NotNull(message = "Giá trị giảm không được để trống")
        @DecimalMin(value = "0.01", message = "Giá trị giảm phải lớn hơn 0") BigDecimal discountValue,
        @DecimalMin(value = "0", inclusive = true, message = "Mức giảm tối đa không được âm") BigDecimal maxDiscount,
        @DecimalMin(value = "0", inclusive = true, message = "Đơn tối thiểu không được âm") BigDecimal minimumOrder,
        @Positive(message = "Giới hạn lượt dùng phải lớn hơn 0") Integer usageLimit,
        @Positive(message = "Giới hạn mỗi khách hàng phải lớn hơn 0") Integer perUserLimit,
        @NotNull(message = "Thời gian bắt đầu không được để trống") Instant startsAt,
        @NotNull(message = "Thời gian kết thúc không được để trống") Instant endsAt,
        Boolean active
) {
}

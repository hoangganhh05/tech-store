package com.techstore.dto.response;

import com.techstore.entity.Voucher;
import com.techstore.enums.DiscountType;

import java.math.BigDecimal;
import java.time.Instant;

public record VoucherResponse(
        Long id, String code, String name, DiscountType discountType, BigDecimal discountValue,
        BigDecimal maxDiscount, BigDecimal minimumOrder, Integer usageLimit, Integer perUserLimit,
        Integer usedCount, Instant startsAt, Instant endsAt, boolean active, Instant createdAt, Instant updatedAt
) {
    public static VoucherResponse from(Voucher voucher) {
        return new VoucherResponse(voucher.getId(), voucher.getCode(), voucher.getName(), voucher.getDiscountType(),
                voucher.getDiscountValue(), voucher.getMaxDiscount(), voucher.getMinimumOrder(), voucher.getUsageLimit(),
                voucher.getPerUserLimit(), voucher.getUsedCount(), voucher.getStartsAt(), voucher.getEndsAt(),
                voucher.isActive(), voucher.getCreatedAt(), voucher.getUpdatedAt());
    }
}

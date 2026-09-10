package com.techstore.service;

import com.techstore.dto.response.CartResponse;
import com.techstore.dto.response.CartItemResponse;
import com.techstore.entity.Voucher;
import com.techstore.enums.DiscountType;
import com.techstore.enums.ErrorCode;
import com.techstore.exception.BusinessException;
import com.techstore.repository.VoucherRepository;
import com.techstore.repository.VoucherUsageRepository;
import com.techstore.service.impl.VoucherServiceImpl;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class VoucherServiceImplTest {
    private final VoucherRepository vouchers = mock(VoucherRepository.class);
    private final VoucherUsageRepository usages = mock(VoucherUsageRepository.class);
    private final CartService carts = mock(CartService.class);
    private final VoucherServiceImpl service = new VoucherServiceImpl(vouchers, usages, carts);

    private Voucher voucher(DiscountType type, BigDecimal value) throws Exception {
        Voucher voucher = new Voucher("SAVE10", "Giảm giá", type, value, null, BigDecimal.ZERO,
                10, 1, Instant.now().minus(1, ChronoUnit.DAYS), Instant.now().plus(1, ChronoUnit.DAYS), true);
        Field id = Voucher.class.getDeclaredField("id");
        id.setAccessible(true); id.set(voucher, 9L);
        return voucher;
    }

    private CartResponse cart() {
        return new CartResponse(1L, 2, new BigDecimal("100000"), new BigDecimal("30000"), BigDecimal.ZERO,
                new BigDecimal("130000"), false, true, List.of(new CartItemResponse(1L, 2L, 3L, "Phone", "SKU",
                        null, null, new BigDecimal("100000"), null, null, 1, 2, new BigDecimal("100000"), false, null)));
    }

    @Test
    void appliesPercentVoucherAndCapsDiscountAtSubtotal() throws Exception {
        Voucher voucher = voucher(DiscountType.PERCENT, new BigDecimal("20"));
        when(vouchers.findByCodeIgnoreCase("SAVE10")).thenReturn(Optional.of(voucher));
        when(usages.countByVoucherIdAndUserId(9L, 7L)).thenReturn(0L);

        var result = service.applyToCart(7L, cart(), " save10 ");

        assertThat(result.discountAmount()).isEqualByComparingTo("20000");
        assertThat(result.total()).isEqualByComparingTo("110000");
    }

    @Test
    void rejectsExpiredVoucherWithClearBusinessError() throws Exception {
        Voucher voucher = new Voucher("OLD", "Cũ", DiscountType.FIXED, new BigDecimal("1000"), null,
                BigDecimal.ZERO, null, 1, Instant.now().minus(2, ChronoUnit.DAYS), Instant.now().minus(1, ChronoUnit.DAYS), true);
        Field id = Voucher.class.getDeclaredField("id"); id.setAccessible(true); id.set(voucher, 10L);
        when(vouchers.findByCodeIgnoreCase("OLD")).thenReturn(Optional.of(voucher));

        assertThatThrownBy(() -> service.applyToCart(7L, cart(), "OLD"))
                .isInstanceOfSatisfying(BusinessException.class, ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.VOUCHER_NOT_ELIGIBLE))
                .hasMessageContaining("hết hạn");
    }

    @Test
    void rejectsOrderBelowMinimum() throws Exception {
        Voucher voucher = voucher(DiscountType.FIXED, new BigDecimal("1000"));
        Field minimum = Voucher.class.getDeclaredField("minimumOrder"); minimum.setAccessible(true); minimum.set(voucher, new BigDecimal("200000"));
        when(vouchers.findByCodeIgnoreCase("SAVE10")).thenReturn(Optional.of(voucher));

        assertThatThrownBy(() -> service.applyToCart(7L, cart(), "SAVE10"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("Đơn hàng tối thiểu");
    }

    @Test
    void rejectsVoucherWhenGlobalUsageLimitIsExhausted() throws Exception {
        Voucher voucher = voucher(DiscountType.FIXED, new BigDecimal("1000"));
        Field usedCount = Voucher.class.getDeclaredField("usedCount");
        usedCount.setAccessible(true);
        usedCount.set(voucher, 10);
        when(vouchers.findByCodeIgnoreCase("SAVE10")).thenReturn(Optional.of(voucher));

        assertThatThrownBy(() -> service.applyToCart(7L, cart(), "SAVE10"))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.VOUCHER_USAGE_LIMIT_REACHED))
                .hasMessageContaining("hết lượt");
    }

    @Test
    void rejectsInactiveVoucherBeforeCalculatingDiscount() throws Exception {
        Voucher voucher = new Voucher("SAVE10", "Giảm giá", DiscountType.FIXED, new BigDecimal("1000"), null,
                BigDecimal.ZERO, 10, 1, Instant.now().minus(1, ChronoUnit.DAYS), Instant.now().plus(1, ChronoUnit.DAYS), false);
        Field id = Voucher.class.getDeclaredField("id");
        id.setAccessible(true);
        id.set(voucher, 11L);
        when(vouchers.findByCodeIgnoreCase("SAVE10")).thenReturn(Optional.of(voucher));

        assertThatThrownBy(() -> service.applyToCart(7L, cart(), "SAVE10"))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.VOUCHER_NOT_ELIGIBLE))
                .hasMessageContaining("hết hạn");
    }

    @Test
    void redeemsVoucherThroughLockedCodeLookup() throws Exception {
        Voucher voucher = voucher(DiscountType.FIXED, new BigDecimal("1000"));
        when(vouchers.findByCodeIgnoreCaseForUpdate("SAVE10")).thenReturn(Optional.of(voucher));
        when(usages.countByVoucherIdAndUserId(9L, 7L)).thenReturn(0L);

        var redemption = service.redeem(7L, cart(), " save10 ");

        assertThat(redemption.voucher()).isSameAs(voucher);
        verify(vouchers).findByCodeIgnoreCaseForUpdate("SAVE10");
        verify(vouchers, never()).findByCodeIgnoreCase("SAVE10");
    }
}

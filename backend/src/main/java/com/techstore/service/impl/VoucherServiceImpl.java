package com.techstore.service.impl;

import com.techstore.dto.response.CartResponse;
import com.techstore.dto.response.VoucherApplicationResponse;
import com.techstore.entity.Order;
import com.techstore.entity.User;
import com.techstore.entity.Voucher;
import com.techstore.entity.VoucherUsage;
import com.techstore.enums.DiscountType;
import com.techstore.enums.ErrorCode;
import com.techstore.exception.BusinessException;
import com.techstore.repository.VoucherRepository;
import com.techstore.repository.VoucherUsageRepository;
import com.techstore.service.CartService;
import com.techstore.service.VoucherRedemption;
import com.techstore.service.VoucherService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Locale;

@Service
public class VoucherServiceImpl implements VoucherService {
    private final VoucherRepository vouchers;
    private final VoucherUsageRepository usages;
    private final CartService cartService;

    public VoucherServiceImpl(VoucherRepository vouchers, VoucherUsageRepository usages, CartService cartService) {
        this.vouchers = vouchers;
        this.usages = usages;
        this.cartService = cartService;
    }

    @Override
    @Transactional(readOnly = true)
    public VoucherApplicationResponse apply(Long userId, String code) {
        CartResponse cart = cartService.getCart(userId, null);
        if (cart.id() == null || cart.items().isEmpty()) {
            throw new BusinessException(ErrorCode.CART_NOT_FOUND, "Giỏ hàng không có sản phẩm để áp dụng voucher");
        }
        VoucherRedemption redemption = validate(userId, cart, code);
        return toResponse(redemption.cart(), redemption.voucher());
    }

    @Override
    @Transactional(readOnly = true)
    public CartResponse applyToCart(Long userId, CartResponse cart, String code) {
        return validate(userId, cart, code).cart();
    }

    @Override
    @Transactional
    public VoucherRedemption redeem(Long userId, CartResponse cart, String code) {
        String normalized = normalize(code);
        Voucher voucher = vouchers.findByCodeIgnoreCaseForUpdate(normalized).orElseThrow(
                () -> new BusinessException(ErrorCode.VOUCHER_NOT_FOUND, "Mã voucher không tồn tại"));
        return validateWithVoucher(userId, cart, voucher);
    }

    @Override
    public void recordUsage(User user, Order order, VoucherRedemption redemption) {
        Voucher voucher = redemption.voucher();
        voucher.incrementUsedCount();
        usages.save(new VoucherUsage(voucher, user, order, redemption.cart().discountAmount()));
    }

    private VoucherRedemption validate(Long userId, CartResponse cart, String code) {
        String normalized = normalize(code);
        Voucher voucher = find(normalized);
        return validateWithVoucher(userId, cart, voucher);
    }

    private VoucherRedemption validateWithVoucher(Long userId, CartResponse cart, Voucher voucher) {
        if (cart == null || cart.items() == null || cart.items().isEmpty()) {
            throw new BusinessException(ErrorCode.CART_NOT_FOUND, "Giỏ hàng không có sản phẩm để áp dụng voucher");
        }
        Instant now = Instant.now();
        if (!voucher.isActive() || now.isBefore(voucher.getStartsAt()) ||
                (voucher.getEndsAt() != null && !now.isBefore(voucher.getEndsAt()))) {
            throw new BusinessException(ErrorCode.VOUCHER_NOT_ELIGIBLE, "Mã voucher đã hết hạn hoặc chưa đến thời gian áp dụng");
        }
        if (voucher.getUsageLimit() != null && voucher.getUsedCount() >= voucher.getUsageLimit()) {
            throw new BusinessException(ErrorCode.VOUCHER_USAGE_LIMIT_REACHED, "Mã voucher đã hết lượt sử dụng");
        }
        if (voucher.getPerUserLimit() != null && usages.countByVoucherIdAndUserId(voucher.getId(), userId) >= voucher.getPerUserLimit()) {
            throw new BusinessException(ErrorCode.VOUCHER_USAGE_LIMIT_REACHED, "Bạn đã sử dụng mã voucher này tối đa số lần cho phép");
        }
        BigDecimal subtotal = cart.subtotal() == null ? BigDecimal.ZERO : cart.subtotal();
        if (subtotal.compareTo(voucher.getMinimumOrder()) < 0) {
            throw new BusinessException(ErrorCode.VOUCHER_NOT_ELIGIBLE,
                    "Đơn hàng tối thiểu để dùng mã này là " + formatMoney(voucher.getMinimumOrder()));
        }
        BigDecimal discount = voucher.getDiscountType() == DiscountType.PERCENT
                ? subtotal.multiply(voucher.getDiscountValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                : voucher.getDiscountValue();
        if (voucher.getMaxDiscount() != null) discount = discount.min(voucher.getMaxDiscount());
        discount = discount.max(BigDecimal.ZERO).min(subtotal).setScale(2, RoundingMode.HALF_UP);
        BigDecimal shipping = cart.shippingFee() == null ? BigDecimal.ZERO : cart.shippingFee();
        BigDecimal total = subtotal.subtract(discount).add(shipping).setScale(2, RoundingMode.HALF_UP);
        CartResponse discounted = new CartResponse(cart.id(), cart.totalItems(), subtotal, shipping, discount, total,
                cart.hasStockIssue(), cart.canCheckout(), cart.items());
        return new VoucherRedemption(voucher, discounted);
    }

    private Voucher find(String code) {
        return vouchers.findByCodeIgnoreCase(code).orElseThrow(
                () -> new BusinessException(ErrorCode.VOUCHER_NOT_FOUND, "Mã voucher không tồn tại"));
    }

    private VoucherApplicationResponse toResponse(CartResponse cart, Voucher voucher) {
        return new VoucherApplicationResponse(voucher.getCode(), voucher.getName(), voucher.getDiscountType(),
                cart.discountAmount(), cart.subtotal(), cart.shippingFee(), cart.total());
    }

    private String normalize(String code) {
        if (code == null || code.isBlank()) throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Vui lòng nhập mã voucher");
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private String formatMoney(BigDecimal amount) { return amount.stripTrailingZeros().toPlainString() + "đ"; }
}

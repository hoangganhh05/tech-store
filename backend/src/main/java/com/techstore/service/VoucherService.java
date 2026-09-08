package com.techstore.service;

import com.techstore.dto.response.CartResponse;
import com.techstore.dto.response.VoucherApplicationResponse;
import com.techstore.entity.Order;
import com.techstore.entity.User;

public interface VoucherService {
    VoucherApplicationResponse apply(Long userId, String code);
    CartResponse applyToCart(Long userId, CartResponse cart, String code);
    VoucherRedemption redeem(Long userId, CartResponse cart, String code);
    void recordUsage(User user, Order order, VoucherRedemption redemption);
}

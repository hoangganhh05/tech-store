package com.techstore.service;

import com.techstore.dto.response.CartResponse;
import com.techstore.entity.Voucher;

public record VoucherRedemption(Voucher voucher, CartResponse cart) {}

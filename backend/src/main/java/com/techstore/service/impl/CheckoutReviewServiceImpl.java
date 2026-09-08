package com.techstore.service.impl;

import com.techstore.dto.request.CheckoutReviewRequest;
import com.techstore.dto.response.AddressResponse;
import com.techstore.dto.response.CartResponse;
import com.techstore.dto.response.CheckoutReviewResponse;
import com.techstore.dto.response.VoucherApplicationResponse;
import com.techstore.entity.Address;
import com.techstore.enums.ErrorCode;
import com.techstore.exception.BusinessException;
import com.techstore.repository.AddressRepository;
import com.techstore.service.CartService;
import com.techstore.service.CheckoutReviewService;
import com.techstore.service.PaymentMethodService;
import com.techstore.service.VoucherService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CheckoutReviewServiceImpl implements CheckoutReviewService {
    private final AddressRepository addressRepository;
    private final CartService cartService;
    private final PaymentMethodService paymentMethodService;
    private final VoucherService voucherService;

    @org.springframework.beans.factory.annotation.Autowired
    public CheckoutReviewServiceImpl(AddressRepository addressRepository, CartService cartService,
                                     PaymentMethodService paymentMethodService, VoucherService voucherService) {
        this.addressRepository = addressRepository;
        this.cartService = cartService;
        this.paymentMethodService = paymentMethodService;
        this.voucherService = voucherService;
    }

    public CheckoutReviewServiceImpl(AddressRepository addressRepository, CartService cartService,
                                     PaymentMethodService paymentMethodService) {
        this(addressRepository, cartService, paymentMethodService, null);
    }

    @Override
    @Transactional(readOnly = true)
    public CheckoutReviewResponse review(Long userId, CheckoutReviewRequest request) {
        Address address = addressRepository.findByIdAndUserId(request.addressId(), userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADDRESS_NOT_FOUND,
                        "Địa chỉ giao hàng không tồn tại hoặc không thuộc tài khoản hiện tại"));
        CartResponse cart = cartService.getCart(userId, null);
        if (cart.id() == null || cart.totalItems() == null || cart.totalItems() <= 0) {
            throw new BusinessException(ErrorCode.CART_NOT_FOUND, "Giỏ hàng không có sản phẩm để đặt hàng");
        }
        VoucherApplicationResponse voucher = null;
        if (request.voucherCode() != null && !request.voucherCode().isBlank()) {
            if (voucherService == null) throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Voucher service chưa được cấu hình");
            voucher = voucherService.apply(userId, request.voucherCode());
            cart = new CartResponse(cart.id(), cart.totalItems(), cart.subtotal(), cart.shippingFee(),
                    voucher.discountAmount(), voucher.total(), cart.hasStockIssue(), cart.canCheckout(), cart.items());
        }
        AddressResponse addressResponse = new AddressResponse(address.getId(), address.getRecipientName(),
                address.getPhone(), address.getProvince(), address.getDistrict(), address.getWard(),
                address.getStreetAddress(), address.isDefault(), address.getCreatedAt());
        return new CheckoutReviewResponse(cart, addressResponse,
                paymentMethodService.select(request.paymentMethod()), cart.canCheckout(), voucher);
    }
}

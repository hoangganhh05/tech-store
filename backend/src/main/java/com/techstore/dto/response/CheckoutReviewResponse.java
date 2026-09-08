package com.techstore.dto.response;

public record CheckoutReviewResponse(
        CartResponse cart,
        AddressResponse shippingAddress,
        PaymentMethodResponse paymentMethod,
        boolean readyToPlaceOrder
) {}

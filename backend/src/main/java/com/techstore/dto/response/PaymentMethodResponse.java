package com.techstore.dto.response;

import com.techstore.enums.PaymentMethod;

public record PaymentMethodResponse(PaymentMethod paymentMethod, String label, String instructions) {}

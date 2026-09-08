package com.techstore.service;

import com.techstore.dto.response.PaymentMethodResponse;
import com.techstore.enums.PaymentMethod;
import java.util.List;

public interface PaymentMethodService {
    List<PaymentMethodResponse> getMethods();
    PaymentMethodResponse select(PaymentMethod method);
}

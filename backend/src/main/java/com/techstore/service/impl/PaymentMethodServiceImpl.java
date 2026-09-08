package com.techstore.service.impl;

import com.techstore.dto.response.PaymentMethodResponse;
import com.techstore.enums.ErrorCode;
import com.techstore.enums.PaymentMethod;
import com.techstore.exception.BusinessException;
import com.techstore.service.PaymentMethodService;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class PaymentMethodServiceImpl implements PaymentMethodService {
    private static final List<PaymentMethodResponse> METHODS = List.of(
            new PaymentMethodResponse(PaymentMethod.COD, "Thanh toán khi nhận hàng (COD)",
                    "Thanh toán cho nhân viên giao hàng khi nhận được đơn hàng."),
            new PaymentMethodResponse(PaymentMethod.BANK_TRANSFER, "Chuyển khoản ngân hàng (giả lập)",
                    "Thông tin giả lập: Ngân hàng DEMO BANK · Số tài khoản DEMO-TECHSTORE-001 · Chủ tài khoản TECHSTORE DEMO. Không chuyển tiền thật."),
            new PaymentMethodResponse(PaymentMethod.ONLINE, "Thanh toán online (giả lập)",
                    "Cổng thanh toán giả lập: lựa chọn này không thu tiền và không xác nhận đơn hàng đã thanh toán.")
    );

    public List<PaymentMethodResponse> getMethods() { return METHODS; }

    public PaymentMethodResponse select(PaymentMethod method) {
        return METHODS.stream().filter(option -> option.paymentMethod() == method).findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_ERROR,
                        "Phương thức thanh toán không hợp lệ"));
    }
}

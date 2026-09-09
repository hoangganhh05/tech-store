package com.techstore.dto.response;

import com.techstore.entity.Order;

import java.math.BigDecimal;
import java.time.Instant;

public record AdminOrderSummaryResponse(
        Long id,
        String orderNumber,
        String customerName,
        String customerPhone,
        BigDecimal totalAmount,
        String status,
        Instant placedAt
) {
    public static AdminOrderSummaryResponse from(Order order) {
        return new AdminOrderSummaryResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getUser().getFullName(),
                order.getUser().getPhone(),
                order.getTotalAmount(),
                order.getStatus(),
                order.getPlacedAt()
        );
    }
}

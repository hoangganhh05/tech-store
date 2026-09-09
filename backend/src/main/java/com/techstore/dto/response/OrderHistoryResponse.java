package com.techstore.dto.response;

import com.techstore.entity.Order;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderHistoryResponse(
        Long id,
        String orderNumber,
        Instant placedAt,
        BigDecimal totalAmount,
        String status
) {
    public static OrderHistoryResponse from(Order order) {
        return new OrderHistoryResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getPlacedAt(),
                order.getTotalAmount(),
                order.getStatus()
        );
    }
}

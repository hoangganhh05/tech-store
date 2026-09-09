package com.techstore.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderStatusUpdatedEvent(
        Long orderId,
        String orderNumber,
        String recipientEmail,
        String recipientName,
        String status,
        String cancellationReason,
        BigDecimal totalAmount,
        String recipientPhone,
        String deliveryAddress,
        Instant changedAt,
        List<ItemSummary> items
) {
    public record ItemSummary(
            String productName,
            String variantLabel,
            Integer quantity,
            BigDecimal unitPrice,
            BigDecimal subtotal
    ) {}
}
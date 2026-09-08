package com.techstore.event;

import com.techstore.dto.response.PlacedOrderItemResponse;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderPlacedEvent(
        String recipientEmail,
        String recipientName,
        String orderNumber,
        BigDecimal totalAmount,
        Instant placedAt,
        String estimatedProcessingTime,
        List<PlacedOrderItemResponse> items
) {}

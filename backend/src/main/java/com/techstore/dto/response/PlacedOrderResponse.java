package com.techstore.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record PlacedOrderResponse(
        Long id,
        String orderNumber,
        String status,
        BigDecimal totalAmount,
        Instant placedAt,
        String estimatedProcessingTime,
        List<PlacedOrderItemResponse> items
) {}

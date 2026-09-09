package com.techstore.dto.response;

public record OrderCancellationResponse(
        Long id,
        String orderNumber,
        String status,
        String cancellationReason
) {
}

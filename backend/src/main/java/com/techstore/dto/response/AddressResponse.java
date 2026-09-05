package com.techstore.dto.response;

import java.time.Instant;

public record AddressResponse(
        Long id,
        String recipientName,
        String phone,
        String province,
        String district,
        String ward,
        String streetAddress,
        boolean isDefault,
        Instant createdAt
) {
}

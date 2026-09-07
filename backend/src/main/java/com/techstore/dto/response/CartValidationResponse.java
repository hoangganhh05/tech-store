package com.techstore.dto.response;

import java.util.List;

public record CartValidationResponse(
        boolean valid,
        List<CartItemStockIssueResponse> issues
) {
}


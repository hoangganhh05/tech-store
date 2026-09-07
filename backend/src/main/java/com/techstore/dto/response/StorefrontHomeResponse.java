package com.techstore.dto.response;

import java.util.List;

public record StorefrontHomeResponse(
        List<StorefrontProductResponse> featuredProducts,
        List<StorefrontProductResponse> newArrivals,
        List<StorefrontProductResponse> onSaleProducts,
        List<CategoryResponse> featuredCategories
) {
}


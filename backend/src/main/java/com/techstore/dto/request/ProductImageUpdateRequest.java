package com.techstore.dto.request;

public record ProductImageUpdateRequest(
        Long variantId,
        Integer displayOrder
) {
}

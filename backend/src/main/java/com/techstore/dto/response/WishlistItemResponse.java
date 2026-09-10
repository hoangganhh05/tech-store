package com.techstore.dto.response;

public record WishlistItemResponse(
        Long productId,
        boolean favorite
) {
}

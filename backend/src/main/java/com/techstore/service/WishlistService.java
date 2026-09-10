package com.techstore.service;

import com.techstore.dto.response.PageResponse;
import com.techstore.dto.response.StorefrontProductResponse;
import com.techstore.dto.response.WishlistItemResponse;

public interface WishlistService {

    PageResponse<StorefrontProductResponse> getWishlist(Long userId, int page, int size);

    WishlistItemResponse addToWishlist(Long userId, Long productId);

    WishlistItemResponse removeFromWishlist(Long userId, Long productId);
}

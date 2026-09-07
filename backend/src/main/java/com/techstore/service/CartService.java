package com.techstore.service;

import com.techstore.dto.request.AddToCartRequest;
import com.techstore.dto.request.UpdateCartItemRequest;
import com.techstore.dto.response.CartResponse;

public interface CartService {

    CartResponse addToCart(Long userId, String sessionId, AddToCartRequest request);

    CartResponse getCart(Long userId, String sessionId);

    CartResponse updateCartItemQuantity(Long userId, String sessionId, Long itemId, UpdateCartItemRequest request);
}


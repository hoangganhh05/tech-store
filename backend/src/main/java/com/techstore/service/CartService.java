package com.techstore.service;

import com.techstore.dto.request.AddToCartRequest;
import com.techstore.dto.request.UpdateCartItemRequest;
import com.techstore.dto.response.CartResponse;
import com.techstore.dto.response.CartSyncResponse;
import com.techstore.dto.response.CartValidationResponse;

public interface CartService {

    CartResponse addToCart(Long userId, String sessionId, AddToCartRequest request);

    CartResponse getCart(Long userId, String sessionId);

    CartResponse updateCartItemQuantity(Long userId, String sessionId, Long itemId, UpdateCartItemRequest request);

    CartResponse removeCartItem(Long userId, String sessionId, Long itemId);

    CartValidationResponse validateCartStock(Long userId, String sessionId);

    CartSyncResponse syncCart(Long userId, String sessionId);
}

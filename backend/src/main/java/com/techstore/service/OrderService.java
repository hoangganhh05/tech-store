package com.techstore.service;

import com.techstore.dto.request.PlaceOrderRequest;
import com.techstore.dto.response.PlacedOrderResponse;

public interface OrderService {
    PlacedOrderResponse placeOrder(Long userId, PlaceOrderRequest request);
}

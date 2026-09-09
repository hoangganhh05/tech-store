package com.techstore.service;

import com.techstore.dto.request.PlaceOrderRequest;
import com.techstore.dto.response.OrderHistoryResponse;
import com.techstore.dto.response.OrderDetailResponse;
import com.techstore.dto.response.PageResponse;
import com.techstore.dto.response.PlacedOrderResponse;

public interface OrderService {
    PlacedOrderResponse placeOrder(Long userId, PlaceOrderRequest request);

    PageResponse<OrderHistoryResponse> getMyOrders(Long userId, String status, int page, int size);

    OrderDetailResponse getOrderDetail(Long userId, Long orderId);
}

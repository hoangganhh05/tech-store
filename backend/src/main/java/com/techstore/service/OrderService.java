package com.techstore.service;

import com.techstore.dto.request.PlaceOrderRequest;
import com.techstore.dto.request.CancelOrderRequest;
import com.techstore.dto.request.UpdateOrderStatusRequest;
import com.techstore.dto.response.OrderHistoryResponse;
import com.techstore.dto.response.OrderDetailResponse;
import com.techstore.dto.response.PageResponse;
import com.techstore.dto.response.PlacedOrderResponse;
import com.techstore.dto.response.OrderCancellationResponse;
import com.techstore.dto.response.AdminOrderSummaryResponse;
import com.techstore.dto.response.AdminOrderDetailResponse;

import java.time.LocalDate;

public interface OrderService {
    PlacedOrderResponse placeOrder(Long userId, PlaceOrderRequest request);

    PageResponse<OrderHistoryResponse> getMyOrders(Long userId, String status, int page, int size);

    OrderDetailResponse getOrderDetail(Long userId, Long orderId);

    OrderCancellationResponse cancelOrder(Long userId, Long orderId, CancelOrderRequest request);

    PageResponse<AdminOrderSummaryResponse> getAdminOrders(
            String search,
            String status,
            LocalDate fromDate,
            LocalDate toDate,
            int page,
            int size
    );

    AdminOrderDetailResponse getAdminOrderDetail(Long orderId);

    AdminOrderDetailResponse updateAdminOrderStatus(Long adminUserId, Long orderId, UpdateOrderStatusRequest request);
}

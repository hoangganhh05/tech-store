package com.techstore.dto.response;

import com.techstore.entity.Order;
import com.techstore.entity.OrderAddress;
import com.techstore.entity.OrderItem;
import com.techstore.entity.OrderStatusHistory;
import com.techstore.enums.PaymentMethod;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderDetailResponse(
        Long id,
        String orderNumber,
        String status,
        PaymentMethod paymentMethod,
        String paymentStatus,
        String cancellationReason,
        BigDecimal subtotal,
        BigDecimal discountAmount,
        BigDecimal shippingFee,
        BigDecimal totalAmount,
        Instant placedAt,
        ShippingAddress shippingAddress,
        List<Item> items,
        List<StatusHistory> statusHistory
) {
    public static OrderDetailResponse from(Order order) {
        OrderAddress address = order.getShippingAddress();
        return new OrderDetailResponse(
                order.getId(), order.getOrderNumber(), order.getStatus(), order.getPaymentMethod(), order.getPaymentStatus(), order.getCancellationReason(),
                order.getSubtotal(),
                order.getDiscountAmount(), order.getShippingFee(), order.getTotalAmount(), order.getPlacedAt(),
                address == null ? null : ShippingAddress.from(address),
                order.getItems().stream().map(Item::from).toList(),
                order.getStatusHistory().stream().map(StatusHistory::from).toList()
        );
    }

    public record ShippingAddress(String recipientName, String recipientPhone, String line1, String ward, String district,
                                  String province) {
        static ShippingAddress from(OrderAddress address) {
            return new ShippingAddress(address.getRecipientName(), address.getRecipientPhone(), address.getLine1(),
                    address.getWard(), address.getDistrict(), address.getProvince());
        }
    }

    public record Item(String productName, String sku, String variantLabel, BigDecimal unitPrice, Integer quantity,
                       BigDecimal subtotal) {
        static Item from(OrderItem item) {
            return new Item(item.getProductName(), item.getSku(), item.getVariantLabel(), item.getUnitPrice(),
                    item.getQuantity(), item.getSubtotal());
        }
    }

    public record StatusHistory(String status, Instant changedAt) {
        static StatusHistory from(OrderStatusHistory entry) {
            return new StatusHistory(entry.getStatus(), entry.getChangedAt());
        }
    }
}

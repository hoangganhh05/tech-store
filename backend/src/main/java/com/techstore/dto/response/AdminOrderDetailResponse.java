package com.techstore.dto.response;

import com.techstore.entity.Order;
import com.techstore.entity.OrderAddress;
import com.techstore.entity.OrderItem;
import com.techstore.entity.OrderStatusHistory;
import com.techstore.enums.PaymentMethod;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Complete order data intended for the administration workspace only.
 * Internal notes are deliberately absent from the customer-facing order DTO.
 */
public record AdminOrderDetailResponse(
        Long id,
        String orderNumber,
        Customer customer,
        String status,
        PaymentMethod paymentMethod,
        String paymentStatus,
        String cancellationReason,
        String internalNote,
        BigDecimal subtotal,
        BigDecimal discountAmount,
        BigDecimal shippingFee,
        BigDecimal totalAmount,
        Instant placedAt,
        ShippingAddress shippingAddress,
        List<Item> items,
        List<StatusHistory> statusHistory
) {
    public static AdminOrderDetailResponse from(Order order) {
        OrderAddress address = order.getShippingAddress();
        return new AdminOrderDetailResponse(
                order.getId(),
                order.getOrderNumber(),
                Customer.from(order),
                order.getStatus(),
                order.getPaymentMethod(),
                order.getPaymentStatus(),
                order.getCancellationReason(),
                order.getInternalNote(),
                order.getSubtotal(),
                order.getDiscountAmount(),
                order.getShippingFee(),
                order.getTotalAmount(),
                order.getPlacedAt(),
                address == null ? null : ShippingAddress.from(address),
                order.getItems().stream().map(Item::from).toList(),
                order.getStatusHistory().stream().map(StatusHistory::from).toList()
        );
    }

    public record Customer(Long id, String fullName, String email, String phone) {
        private static Customer from(Order order) {
            return new Customer(
                    order.getUser().getId(),
                    order.getUser().getFullName(),
                    order.getUser().getEmail(),
                    order.getUser().getPhone()
            );
        }
    }

    public record ShippingAddress(
            String recipientName,
            String recipientPhone,
            String line1,
            String ward,
            String district,
            String province
    ) {
        private static ShippingAddress from(OrderAddress address) {
            return new ShippingAddress(
                    address.getRecipientName(),
                    address.getRecipientPhone(),
                    address.getLine1(),
                    address.getWard(),
                    address.getDistrict(),
                    address.getProvince()
            );
        }
    }

    public record Item(
            String productName,
            String sku,
            String variantLabel,
            BigDecimal unitPrice,
            Integer quantity,
            BigDecimal subtotal
    ) {
        private static Item from(OrderItem item) {
            return new Item(
                    item.getProductName(),
                    item.getSku(),
                    item.getVariantLabel(),
                    item.getUnitPrice(),
                    item.getQuantity(),
                    item.getSubtotal()
            );
        }
    }

    public record StatusHistory(String status, Instant changedAt, ChangedBy changedBy) {
        private static StatusHistory from(OrderStatusHistory history) {
            return new StatusHistory(
                    history.getStatus(),
                    history.getChangedAt(),
                    ChangedBy.from(history.getChangedBy())
            );
        }
    }

    public record ChangedBy(Long id, String fullName) {
        private static ChangedBy from(com.techstore.entity.User user) {
            return user == null ? null : new ChangedBy(user.getId(), user.getFullName());
        }
    }
}

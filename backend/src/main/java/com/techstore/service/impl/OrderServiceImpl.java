package com.techstore.service.impl;

import com.techstore.dto.request.OrderInventoryDeductionRequest;
import com.techstore.dto.request.OrderItemStockRequest;
import com.techstore.dto.request.PlaceOrderRequest;
import com.techstore.dto.response.CartResponse;
import com.techstore.dto.response.PlacedOrderResponse;
import com.techstore.entity.*;
import com.techstore.enums.ErrorCode;
import com.techstore.exception.BusinessException;
import com.techstore.repository.*;
import com.techstore.service.CartService;
import com.techstore.service.InventoryService;
import com.techstore.service.OrderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class OrderServiceImpl implements OrderService {
    private final UserRepository users; private final AddressRepository addresses; private final CartRepository carts;
    private final CartItemRepository cartItems; private final OrderRepository orders; private final CartService cartService;
    private final InventoryService inventory;
    public OrderServiceImpl(UserRepository users, AddressRepository addresses, CartRepository carts, CartItemRepository cartItems,
                            OrderRepository orders, CartService cartService, InventoryService inventory) {
        this.users=users; this.addresses=addresses; this.carts=carts; this.cartItems=cartItems; this.orders=orders; this.cartService=cartService; this.inventory=inventory;
    }

    @Override @Transactional
    public PlacedOrderResponse placeOrder(Long userId, PlaceOrderRequest request) {
        User user = users.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.INVALID_ACCESS_TOKEN, "Phiên đăng nhập không hợp lệ hoặc đã hết hạn"));
        Address address = addresses.findByIdAndUserId(request.addressId(), userId).orElseThrow(() -> new BusinessException(ErrorCode.ADDRESS_NOT_FOUND, "Địa chỉ giao hàng không tồn tại hoặc không thuộc tài khoản hiện tại"));
        CartResponse cart = cartService.getCart(userId, null);
        if (cart.id() == null || cart.items().isEmpty()) throw new BusinessException(ErrorCode.CART_NOT_FOUND, "Giỏ hàng không có sản phẩm để đặt hàng");
        if (!cart.canCheckout()) {
            String issues = cart.items().stream().filter(i -> i.hasStockIssue()).map(i -> i.productName() + ": " + i.stockStatusMessage()).reduce((a,b) -> a + "; " + b).orElse("Tồn kho không đủ");
            throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK, issues);
        }
        String number = "TS-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
        Order order = new Order(number, user, request.paymentMethod(), cart.subtotal(), cart.discountAmount(), cart.shippingFee());
        order.setShippingAddress(new OrderAddress(order, address.getRecipientName(), address.getPhone(), address.getStreetAddress(), address.getWard(), address.getDistrict(), address.getProvince()));
        List<OrderItemStockRequest> deductions = cart.items().stream().map(i -> {
            String label = (i.color() == null ? "" : i.color()) + (i.storage() == null ? "" : " / " + i.storage());
            order.addItem(new OrderItem(i.variantId(), i.productName(), i.sku(), label, i.price(), i.quantity()));
            return new OrderItemStockRequest(i.variantId(), i.quantity());
        }).toList();
        orders.saveAndFlush(order);
        inventory.deductInventoryForOrder(userId, new OrderInventoryDeductionRequest(order.getId(), order.getOrderNumber(), deductions, null));
        cartItems.deleteByCartId(cart.id());
        return new PlacedOrderResponse(order.getId(), order.getOrderNumber(), order.getStatus(), order.getTotalAmount(), order.getPlacedAt() == null ? Instant.now() : order.getPlacedAt());
    }
}

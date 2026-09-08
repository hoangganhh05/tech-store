package com.techstore.service.impl;

import com.techstore.dto.request.OrderInventoryDeductionRequest;
import com.techstore.dto.request.OrderItemStockRequest;
import com.techstore.dto.request.PlaceOrderRequest;
import com.techstore.dto.response.CartResponse;
import com.techstore.dto.response.PlacedOrderItemResponse;
import com.techstore.dto.response.PlacedOrderResponse;
import com.techstore.entity.*;
import com.techstore.enums.ErrorCode;
import com.techstore.event.OrderPlacedEvent;
import com.techstore.exception.BusinessException;
import com.techstore.repository.*;
import com.techstore.service.CartService;
import com.techstore.service.InventoryService;
import com.techstore.service.OrderService;
import com.techstore.service.VoucherRedemption;
import com.techstore.service.VoucherService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class OrderServiceImpl implements OrderService {
    private final UserRepository users; private final AddressRepository addresses; private final CartRepository carts;
    private final CartItemRepository cartItems; private final OrderRepository orders; private final CartService cartService;
    private final InventoryService inventory; private final ApplicationEventPublisher eventPublisher;
    private final VoucherService voucherService;
    @org.springframework.beans.factory.annotation.Autowired
    public OrderServiceImpl(UserRepository users, AddressRepository addresses, CartRepository carts, CartItemRepository cartItems,
                            OrderRepository orders, CartService cartService, InventoryService inventory,
                            ApplicationEventPublisher eventPublisher, VoucherService voucherService) {
        this.users=users; this.addresses=addresses; this.carts=carts; this.cartItems=cartItems; this.orders=orders; this.cartService=cartService; this.inventory=inventory; this.eventPublisher=eventPublisher;
        this.voucherService = voucherService;
    }

    public OrderServiceImpl(UserRepository users, AddressRepository addresses, CartRepository carts, CartItemRepository cartItems,
                            OrderRepository orders, CartService cartService, InventoryService inventory,
                            ApplicationEventPublisher eventPublisher) {
        this(users, addresses, carts, cartItems, orders, cartService, inventory, eventPublisher, null);
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
        VoucherRedemption redemption = null;
        if (request.voucherCode() != null && !request.voucherCode().isBlank()) {
            if (voucherService == null) throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Voucher service chưa được cấu hình");
            redemption = voucherService.redeem(userId, cart, request.voucherCode());
            cart = redemption.cart();
        }
        String number = "TS-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
        Order order = new Order(number, user, request.paymentMethod(), cart.subtotal(), cart.discountAmount(), cart.shippingFee());
        if (redemption != null) order.setVoucher(redemption.voucher());
        order.setShippingAddress(new OrderAddress(order, address.getRecipientName(), address.getPhone(), address.getStreetAddress(), address.getWard(), address.getDistrict(), address.getProvince()));
        List<OrderItemStockRequest> deductions = new ArrayList<>();
        List<PlacedOrderItemResponse> confirmationItems = new ArrayList<>();
        cart.items().forEach(i -> {
            String label = (i.color() == null ? "" : i.color()) + (i.storage() == null ? "" : " / " + i.storage());
            order.addItem(new OrderItem(i.variantId(), i.productName(), i.sku(), label, i.price(), i.quantity()));
            deductions.add(new OrderItemStockRequest(i.variantId(), i.quantity()));
            confirmationItems.add(new PlacedOrderItemResponse(i.productName(), label, i.price(), i.quantity(), i.subtotal()));
        });
        orders.saveAndFlush(order);
        if (redemption != null) voucherService.recordUsage(user, order, redemption);
        inventory.deductInventoryForOrder(userId, new OrderInventoryDeductionRequest(order.getId(), order.getOrderNumber(), deductions, null));
        cartItems.deleteByCartId(cart.id());
        Instant placedAt = order.getPlacedAt() == null ? Instant.now() : order.getPlacedAt();
        String estimatedProcessingTime = "1-2 ngày làm việc";
        List<PlacedOrderItemResponse> immutableItems = List.copyOf(confirmationItems);
        eventPublisher.publishEvent(new OrderPlacedEvent(user.getEmail(), user.getFullName(), order.getOrderNumber(),
                order.getTotalAmount(), placedAt, estimatedProcessingTime, immutableItems));
        return new PlacedOrderResponse(order.getId(), order.getOrderNumber(), order.getStatus(), order.getTotalAmount(),
                placedAt, estimatedProcessingTime, immutableItems);
    }
}

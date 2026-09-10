package com.techstore.service;

import com.techstore.dto.request.OrderInventoryDeductionRequest;
import com.techstore.dto.request.PlaceOrderRequest;
import com.techstore.dto.request.UpdateOrderStatusRequest;
import com.techstore.dto.response.CartItemResponse;
import com.techstore.dto.response.CartResponse;
import com.techstore.entity.Address;
import com.techstore.entity.Order;
import com.techstore.entity.User;
import com.techstore.entity.Voucher;
import com.techstore.enums.PaymentMethod;
import com.techstore.enums.DiscountType;
import com.techstore.event.OrderPlacedEvent;
import com.techstore.repository.*;
import com.techstore.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import java.math.BigDecimal;
import java.lang.reflect.Field;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InOrder;
import static org.mockito.Mockito.*;

class OrderServiceImplTest {
    private final UserRepository users = mock(UserRepository.class);
    private final AddressRepository addresses = mock(AddressRepository.class);
    private final CartRepository carts = mock(CartRepository.class);
    private final CartItemRepository cartItems = mock(CartItemRepository.class);
    private final OrderRepository orders = mock(OrderRepository.class);
    private final CartService cartService = mock(CartService.class);
    private final InventoryService inventory = mock(InventoryService.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final VoucherService voucherService = mock(VoucherService.class);
    private final OrderServiceImpl service = new OrderServiceImpl(users, addresses, carts, cartItems, orders, cartService, inventory, eventPublisher, voucherService);

    private final User user = new User("order@example.com", "hash", "Order User", "0912345678");
    private final Address address = new Address(user, "Order User", "0912345678", "Ha Noi", "Cau Giay", "Dich Vong", "1 Duy Tan");

    private CartResponse cart(boolean ready) {
        CartItemResponse item = new CartItemResponse(1L, 101L, 5L, "Phone", "SKU-1", "Black", "128GB",
                new BigDecimal("100000"), null, null, 2, 4, new BigDecimal("200000"), !ready, ready ? null : "Tồn kho không đủ");
        return new CartResponse(10L, 2, new BigDecimal("200000"), new BigDecimal("30000"), BigDecimal.ZERO,
                new BigDecimal("230000"), !ready, ready, List.of(item));
    }

    @Test
    void createsOrderDeductsInventoryAndClearsCartInOrder() {
        when(users.findById(7L)).thenReturn(Optional.of(user));
        when(addresses.findByIdAndUserId(3L, 7L)).thenReturn(Optional.of(address));
        when(cartService.getCart(7L, null)).thenReturn(cart(true));
        when(orders.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.placeOrder(7L, new PlaceOrderRequest(3L, PaymentMethod.COD));

        assertThat(result.orderNumber()).startsWith("TS-");
        verify(orders).saveAndFlush(any());
        verify(inventory).deductInventoryForOrder(eq(7L), any(OrderInventoryDeductionRequest.class));
        verify(cartItems).deleteByCartId(10L);
        verify(eventPublisher).publishEvent(any(OrderPlacedEvent.class));
    }

    @Test
    void blocksAndDoesNotMutateWhenCartHasInsufficientStock() {
        when(users.findById(7L)).thenReturn(Optional.of(user));
        when(addresses.findByIdAndUserId(3L, 7L)).thenReturn(Optional.of(address));
        when(cartService.getCart(7L, null)).thenReturn(cart(false));

        assertThatThrownBy(() -> service.placeOrder(7L, new PlaceOrderRequest(3L, PaymentMethod.COD)))
                .hasMessageContaining("Phone");
        verifyNoInteractions(orders, inventory, cartItems);
    }

    @Test
    void propagatesInventoryFailureSoTransactionCanRollback() {
        when(users.findById(7L)).thenReturn(Optional.of(user));
        when(addresses.findByIdAndUserId(3L, 7L)).thenReturn(Optional.of(address));
        when(cartService.getCart(7L, null)).thenReturn(cart(true));
        when(orders.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new com.techstore.exception.BusinessException(com.techstore.enums.ErrorCode.INSUFFICIENT_STOCK, "SKU-1 không đủ tồn kho"))
                .when(inventory).deductInventoryForOrder(anyLong(), any());

        assertThatThrownBy(() -> service.placeOrder(7L, new PlaceOrderRequest(3L, PaymentMethod.COD)))
                .hasMessageContaining("không đủ tồn kho");
        verify(cartItems, never()).deleteByCartId(anyLong());
    }

    @Test
    void recordsVoucherUsageOnlyAfterInventoryAndCartSucceed() {
        when(users.findById(7L)).thenReturn(Optional.of(user));
        when(addresses.findByIdAndUserId(3L, 7L)).thenReturn(Optional.of(address));
        CartResponse readyCart = cart(true);
        when(cartService.getCart(7L, null)).thenReturn(readyCart);
        when(orders.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        Voucher voucher = new Voucher("SAVE10", "Giảm giá", DiscountType.PERCENT, new BigDecimal("10"), null,
                BigDecimal.ZERO, 10, 1, Instant.now().minus(1, ChronoUnit.DAYS), Instant.now().plus(1, ChronoUnit.DAYS), true);
        CartResponse discountedCart = new CartResponse(10L, 2, new BigDecimal("200000"), new BigDecimal("30000"),
                new BigDecimal("20000"), new BigDecimal("210000"), false, true, cart(true).items());
        VoucherRedemption redemption = new VoucherRedemption(voucher, discountedCart);
        when(voucherService.redeem(7L, readyCart, "SAVE10")).thenReturn(redemption);

        service.placeOrder(7L, new PlaceOrderRequest(3L, PaymentMethod.COD, "SAVE10"));

        InOrder order = inOrder(orders, inventory, cartItems, voucherService);
        order.verify(voucherService).redeem(7L, readyCart, "SAVE10");
        order.verify(orders).saveAndFlush(any());
        order.verify(inventory).deductInventoryForOrder(eq(7L), any(OrderInventoryDeductionRequest.class));
        order.verify(cartItems).deleteByCartId(10L);
        order.verify(voucherService).recordUsage(eq(user), any(), same(redemption));
    }

    @Test
    void allowsAdminToMovePendingOrderToConfirmed() throws Exception {
        User admin = new User("admin@example.com", "hash", "Admin", "0900000000");
        Order order = new Order("TS-100", user, PaymentMethod.COD,
                new BigDecimal("200000"), BigDecimal.ZERO, new BigDecimal("30000"));
        setId(order, 100L);
        when(users.findById(9L)).thenReturn(Optional.of(admin));
        when(orders.findByIdForUpdate(100L)).thenReturn(Optional.of(order));
        when(orders.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.updateAdminOrderStatus(9L, 100L,
                new UpdateOrderStatusRequest(" confirmed ", null));

        assertThat(order.getStatus()).isEqualTo("CONFIRMED");
        assertThat(result.status()).isEqualTo("CONFIRMED");
        verify(orders, atLeastOnce()).saveAndFlush(order);
        verify(eventPublisher).publishEvent(any(com.techstore.event.OrderStatusUpdatedEvent.class));
        verifyNoInteractions(inventory);
    }

    @Test
    void rejectsInvalidStatusTransitionWithoutMutatingOrder() {
        User admin = new User("admin@example.com", "hash", "Admin", "0900000000");
        Order order = new Order("TS-101", user, PaymentMethod.COD,
                new BigDecimal("200000"), BigDecimal.ZERO, new BigDecimal("30000"));
        when(users.findById(9L)).thenReturn(Optional.of(admin));
        when(orders.findByIdForUpdate(101L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.updateAdminOrderStatus(9L, 101L,
                new UpdateOrderStatusRequest("SHIPPING", null)))
                .hasMessageContaining("Không thể chuyển trạng thái từ PENDING sang SHIPPING");
        assertThat(order.getStatus()).isEqualTo("PENDING");
        verify(orders, never()).saveAndFlush(any());
        verifyNoInteractions(inventory, eventPublisher);
    }

    private static void setId(Object entity, long id) throws Exception {
        Field field = entity.getClass().getDeclaredField("id");
        field.setAccessible(true);
        field.set(entity, id);
    }
}

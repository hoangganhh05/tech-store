package com.techstore.service;

import com.techstore.dto.request.OrderInventoryDeductionRequest;
import com.techstore.dto.request.PlaceOrderRequest;
import com.techstore.dto.response.CartItemResponse;
import com.techstore.dto.response.CartResponse;
import com.techstore.entity.Address;
import com.techstore.entity.User;
import com.techstore.enums.PaymentMethod;
import com.techstore.event.OrderPlacedEvent;
import com.techstore.repository.*;
import com.techstore.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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
    private final OrderServiceImpl service = new OrderServiceImpl(users, addresses, carts, cartItems, orders, cartService, inventory, eventPublisher);

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
}

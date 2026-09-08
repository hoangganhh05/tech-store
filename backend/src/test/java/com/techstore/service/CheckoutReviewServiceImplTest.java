package com.techstore.service;

import com.techstore.dto.request.CheckoutReviewRequest;
import com.techstore.dto.response.CartResponse;
import com.techstore.dto.response.CheckoutReviewResponse;
import com.techstore.dto.response.PaymentMethodResponse;
import com.techstore.entity.Address;
import com.techstore.entity.User;
import com.techstore.enums.PaymentMethod;
import com.techstore.exception.BusinessException;
import com.techstore.repository.AddressRepository;
import com.techstore.service.impl.CheckoutReviewServiceImpl;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class CheckoutReviewServiceImplTest {
    private final AddressRepository addresses = mock(AddressRepository.class);
    private final CartService carts = mock(CartService.class);
    private final PaymentMethodService payments = mock(PaymentMethodService.class);
    private final CheckoutReviewServiceImpl service = new CheckoutReviewServiceImpl(addresses, carts, payments);

    @Test
    void aggregatesOwnedAddressPaymentAndCurrentCart() {
        User user = new User("review@example.com", "hash", "Nguyen Van A", "0912345678");
        Address address = new Address(user, "Nguyen Van A", "0912345678", "Ha Noi", "Cau Giay", "Dich Vong", "1 Duy Tan");
        CartResponse cart = new CartResponse(10L, 2, new BigDecimal("50000000"), BigDecimal.ZERO,
                BigDecimal.ZERO, new BigDecimal("50000000"), false, true, List.of());
        PaymentMethodResponse payment = new PaymentMethodResponse(PaymentMethod.COD, "COD", "Pay on delivery");
        when(addresses.findByIdAndUserId(5L, 7L)).thenReturn(Optional.of(address));
        when(carts.getCart(7L, null)).thenReturn(cart);
        when(payments.select(PaymentMethod.COD)).thenReturn(payment);

        CheckoutReviewResponse result = service.review(7L, new CheckoutReviewRequest(5L, PaymentMethod.COD));

        assertThat(result.cart()).isSameAs(cart);
        assertThat(result.shippingAddress().recipientName()).isEqualTo("Nguyen Van A");
        assertThat(result.paymentMethod()).isSameAs(payment);
        assertThat(result.readyToPlaceOrder()).isTrue();
        verify(carts).getCart(7L, null);
    }

    @Test
    void rejectsAddressOwnedByAnotherCustomer() {
        when(addresses.findByIdAndUserId(5L, 7L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.review(7L, new CheckoutReviewRequest(5L, PaymentMethod.COD)))
                .isInstanceOf(BusinessException.class).hasMessageContaining("không thuộc tài khoản");
        verifyNoInteractions(carts, payments);
    }

    @Test
    void rejectsEmptyCart() {
        User user = new User("empty@example.com", "hash", "Empty", "0912345678");
        when(addresses.findByIdAndUserId(5L, 7L)).thenReturn(Optional.of(
                new Address(user, "Empty", "0912345678", "HN", "CG", "DV", "1 DT")));
        when(carts.getCart(7L, null)).thenReturn(new CartResponse(null, 0, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, false, false, List.of()));
        assertThatThrownBy(() -> service.review(7L, new CheckoutReviewRequest(5L, PaymentMethod.COD)))
                .isInstanceOf(BusinessException.class).hasMessageContaining("không có sản phẩm");
        verifyNoInteractions(payments);
    }

    @Test
    void marksReviewNotReadyWhenCartHasStockIssue() {
        User user = new User("stock@example.com", "hash", "Stock", "0912345678");
        when(addresses.findByIdAndUserId(5L, 7L)).thenReturn(Optional.of(
                new Address(user, "Stock", "0912345678", "HN", "CG", "DV", "1 DT")));
        when(carts.getCart(7L, null)).thenReturn(new CartResponse(10L, 1, BigDecimal.TEN,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.TEN, true, false, List.of()));
        when(payments.select(PaymentMethod.ONLINE)).thenReturn(
                new PaymentMethodResponse(PaymentMethod.ONLINE, "Online", "Simulated"));
        assertThat(service.review(7L, new CheckoutReviewRequest(5L, PaymentMethod.ONLINE)).readyToPlaceOrder()).isFalse();
    }
}

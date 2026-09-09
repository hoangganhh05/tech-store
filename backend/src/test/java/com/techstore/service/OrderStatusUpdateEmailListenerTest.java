package com.techstore.service;

import com.techstore.event.OrderStatusUpdatedEvent;
import com.techstore.infrastructure.mail.OrderStatusUpdateEmailSender;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

class OrderStatusUpdateEmailListenerTest {

    private final OrderStatusUpdateEmailSender sender = mock(OrderStatusUpdateEmailSender.class);
    private final OrderStatusUpdateEmailListener listener = new OrderStatusUpdateEmailListener(sender);
    private final OrderStatusUpdatedEvent event = new OrderStatusUpdatedEvent(
            1L,
            "TS-STATUS-001",
            "customer@example.com",
            "Nguyễn Văn A",
            "CONFIRMED",
            null,
            new BigDecimal("500000"),
            "0901234567",
            "123 Cầu Giấy, Hà Nội",
            Instant.now(),
            List.of(new OrderStatusUpdatedEvent.ItemSummary("Tai nghe", "Trắng", 1, new BigDecimal("500000"), new BigDecimal("500000")))
    );

    @Test
    void forwardsStatusUpdateAfterTransactionCommit() {
        listener.sendOrderStatusUpdate(event);
        verify(sender).send(event);
    }

    @Test
    void ignoresSenderFailureWithoutPropagatingToCaller() {
        doThrow(new IllegalStateException("SMTP connection timed out")).when(sender).send(event);
        assertThatCode(() -> listener.sendOrderStatusUpdate(event)).doesNotThrowAnyException();
    }
}

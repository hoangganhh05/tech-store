package com.techstore.service;

import com.techstore.event.OrderPlacedEvent;
import com.techstore.infrastructure.mail.OrderConfirmationEmailSender;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

class OrderConfirmationEmailListenerTest {

    private final OrderConfirmationEmailSender sender = mock(OrderConfirmationEmailSender.class);
    private final OrderConfirmationEmailListener listener = new OrderConfirmationEmailListener(sender);
    private final OrderPlacedEvent event = new OrderPlacedEvent(
            "customer@example.com", "Nguyễn Văn A", "TS-ABC123", BigDecimal.TEN,
            Instant.now(), "1-2 ngày làm việc", List.of()
    );

    @Test
    void forwardsConfirmationAfterOrderCommit() {
        listener.sendConfirmation(event);
        verify(sender).send(event);
    }

    @Test
    void ignoresSenderFailureWithoutPropagatingToOrderFlow() {
        doThrow(new IllegalStateException("mail service down")).when(sender).send(event);
        assertThatCode(() -> listener.sendConfirmation(event)).doesNotThrowAnyException();
    }
}

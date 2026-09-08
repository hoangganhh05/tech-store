package com.techstore.service;

import com.techstore.event.OrderPlacedEvent;
import com.techstore.infrastructure.mail.OrderConfirmationEmailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class OrderConfirmationEmailListener {

    private static final Logger log = LoggerFactory.getLogger(OrderConfirmationEmailListener.class);
    private final OrderConfirmationEmailSender emailSender;

    public OrderConfirmationEmailListener(OrderConfirmationEmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @Async("orderEmailTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void sendConfirmation(OrderPlacedEvent event) {
        try {
            emailSender.send(event);
        } catch (Exception exception) {
            log.error("Unable to send order confirmation email for {}", event.orderNumber(), exception);
        }
    }
}

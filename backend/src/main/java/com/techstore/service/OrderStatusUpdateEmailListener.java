package com.techstore.service;

import com.techstore.event.OrderStatusUpdatedEvent;
import com.techstore.infrastructure.mail.OrderStatusUpdateEmailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class OrderStatusUpdateEmailListener {

    private static final Logger log = LoggerFactory.getLogger(OrderStatusUpdateEmailListener.class);
    private final OrderStatusUpdateEmailSender emailSender;

    public OrderStatusUpdateEmailListener(OrderStatusUpdateEmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @Async("orderEmailTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void sendOrderStatusUpdate(OrderStatusUpdatedEvent event) {
        try {
            emailSender.send(event);
        } catch (Exception exception) {
            log.error("Unable to send order status update email for {}", event.orderNumber(), exception);
        }
    }
}

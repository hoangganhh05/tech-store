package com.techstore.infrastructure.mail;

import com.techstore.event.OrderStatusUpdatedEvent;

public interface OrderStatusUpdateEmailSender {
    void send(OrderStatusUpdatedEvent event);
}
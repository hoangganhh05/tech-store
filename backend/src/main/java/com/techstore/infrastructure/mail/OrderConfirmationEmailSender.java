package com.techstore.infrastructure.mail;

import com.techstore.event.OrderPlacedEvent;

public interface OrderConfirmationEmailSender {
    void send(OrderPlacedEvent event);
}

package com.techstore.service;

import com.techstore.entity.Order;
import com.techstore.event.OrderPlacedEvent;
import com.techstore.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class OrderNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(OrderNotificationListener.class);

    private final NotificationService notificationService;
    private final OrderRepository orderRepository;

    public OrderNotificationListener(NotificationService notificationService, OrderRepository orderRepository) {
        this.notificationService = notificationService;
        this.orderRepository = orderRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderPlaced(OrderPlacedEvent event) {
        try {
            Order order = orderRepository.findByOrderNumber(event.orderNumber()).orElse(null);
            if (order != null) {
                notificationService.createOrderNotification(order);
            } else {
                log.warn("Could not find order {} to create admin notification", event.orderNumber());
            }
        } catch (Exception exception) {
            log.error("Failed to create admin notification for placed order {}", event.orderNumber(), exception);
        }
    }
}

package com.techstore.dto.response;

import com.techstore.entity.Notification;
import java.time.Instant;

public record NotificationResponse(
        Long id,
        String title,
        String message,
        String type,
        Long orderId,
        String orderNumber,
        boolean isRead,
        Instant createdAt,
        Instant readAt
) {
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getType(),
                notification.getOrder() != null ? notification.getOrder().getId() : null,
                notification.getOrder() != null ? notification.getOrder().getOrderNumber() : null,
                notification.isRead(),
                notification.getCreatedAt(),
                notification.getReadAt()
        );
    }
}

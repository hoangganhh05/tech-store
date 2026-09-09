package com.techstore.service;

import com.techstore.dto.response.AdminNotificationListResponse;
import com.techstore.dto.response.NotificationResponse;
import com.techstore.entity.Order;

public interface NotificationService {
    void createOrderNotification(Order order);
    AdminNotificationListResponse getAdminNotifications(Long adminUserId, int page, int size);
    NotificationResponse markAsRead(Long adminUserId, Long notificationId);
    int markAllAsRead(Long adminUserId);
}

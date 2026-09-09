package com.techstore.dto.response;

import java.util.List;

public record AdminNotificationListResponse(
        List<NotificationResponse> items,
        long unreadCount,
        int page,
        int size,
        long totalElements,
        int totalPages
) {}

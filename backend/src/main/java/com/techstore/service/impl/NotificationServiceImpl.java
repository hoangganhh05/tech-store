package com.techstore.service.impl;

import com.techstore.dto.response.AdminNotificationListResponse;
import com.techstore.dto.response.NotificationResponse;
import com.techstore.entity.Notification;
import com.techstore.entity.Order;
import com.techstore.entity.User;
import com.techstore.enums.ErrorCode;
import com.techstore.enums.RoleCode;
import com.techstore.exception.BusinessException;
import com.techstore.repository.NotificationRepository;
import com.techstore.repository.UserRepository;
import com.techstore.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);
    private static final int MAX_PAGE_SIZE = 50;

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationServiceImpl(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public void createOrderNotification(Order order) {
        if (order == null) return;
        List<User> admins = userRepository.findByRoleCode(RoleCode.ADMIN);
        if (admins.isEmpty()) {
            log.warn("No admin accounts found to receive order notification for order {}", order.getOrderNumber());
            return;
        }

        String customerName = order.getUser() != null ? order.getUser().getFullName() : "Khách hàng";
        String title = "Đơn hàng mới #" + order.getOrderNumber();
        String message = "Khách hàng " + customerName + " đã đặt đơn hàng trị giá " + order.getTotalAmount() + " VND.";

        List<Notification> notifications = admins.stream()
                .map(admin -> new Notification(admin, title, message, "ORDER_CREATED", order))
                .toList();

        notificationRepository.saveAll(notifications);
        log.info("Created order notifications for {} admin(s) for order {}", admins.size(), order.getOrderNumber());
    }

    @Override
    @Transactional(readOnly = true)
    public AdminNotificationListResponse getAdminNotifications(Long adminUserId, int page, int size) {
        if (page < 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Số trang không được âm");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Kích thước trang phải từ 1 đến " + MAX_PAGE_SIZE);
        }

        PageRequest pageable = PageRequest.of(page, size);
        Page<Notification> pageResult = notificationRepository.findByUserIdOrderByCreatedAtDesc(adminUserId, pageable);
        long unreadCount = notificationRepository.countByUserIdAndIsReadFalse(adminUserId);

        List<NotificationResponse> items = pageResult.getContent().stream()
                .map(NotificationResponse::from)
                .toList();

        return new AdminNotificationListResponse(
                items,
                unreadCount,
                pageResult.getNumber(),
                pageResult.getSize(),
                pageResult.getTotalElements(),
                pageResult.getTotalPages()
        );
    }

    @Override
    @Transactional
    public NotificationResponse markAsRead(Long adminUserId, Long notificationId) {
        if (notificationId == null || notificationId < 1) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Mã thông báo không hợp lệ");
        }

        Notification notification = notificationRepository.findByIdAndUserId(notificationId, adminUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_ERROR, "Thông báo không tồn tại"));

        if (!notification.isRead()) {
            notification.markAsRead();
            notification = notificationRepository.save(notification);
        }

        return NotificationResponse.from(notification);
    }

    @Override
    @Transactional
    public int markAllAsRead(Long adminUserId) {
        return notificationRepository.markAllAsReadByUserId(adminUserId, Instant.now());
    }
}

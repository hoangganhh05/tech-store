package com.techstore.controller.admin;

import com.techstore.dto.response.AdminNotificationListResponse;
import com.techstore.dto.response.ApiResponse;
import com.techstore.dto.response.NotificationResponse;
import com.techstore.enums.RoleCode;
import com.techstore.security.RequireRole;
import com.techstore.security.RoleAuthorizationInterceptor;
import com.techstore.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${app.api.base-path}/admin/notifications")
@RequireRole(RoleCode.ADMIN)
@Validated
@Tag(name = "Admin Notifications", description = "Quản lý thông báo in-app cho quản trị viên")
public class AdminNotificationController {

    private final NotificationService notificationService;

    public AdminNotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    @Operation(summary = "Xem danh sách thông báo và số lượng chưa đọc của admin")
    public ApiResponse<AdminNotificationListResponse> getNotifications(
            @RequestAttribute(RoleAuthorizationInterceptor.CURRENT_USER_ID_ATTRIBUTE) Long adminUserId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        AdminNotificationListResponse response = notificationService.getAdminNotifications(adminUserId, page, size);
        return ApiResponse.success("Lấy danh sách thông báo thành công", response);
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Đánh dấu một thông báo là đã đọc")
    public ApiResponse<NotificationResponse> markAsRead(
            @RequestAttribute(RoleAuthorizationInterceptor.CURRENT_USER_ID_ATTRIBUTE) Long adminUserId,
            @PathVariable @Positive(message = "Mã thông báo không hợp lệ") Long id
    ) {
        NotificationResponse response = notificationService.markAsRead(adminUserId, id);
        return ApiResponse.success("Đánh dấu thông báo đã đọc thành công", response);
    }

    @PatchMapping("/read-all")
    @Operation(summary = "Đánh dấu tất cả thông báo là đã đọc")
    public ApiResponse<Integer> markAllAsRead(
            @RequestAttribute(RoleAuthorizationInterceptor.CURRENT_USER_ID_ATTRIBUTE) Long adminUserId
    ) {
        int updatedCount = notificationService.markAllAsRead(adminUserId);
        return ApiResponse.success("Đã đánh dấu tất cả thông báo là đã đọc", updatedCount);
    }
}

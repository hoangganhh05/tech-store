package com.techstore.controller.admin;

import com.techstore.dto.request.UpdateUserStatusRequest;
import com.techstore.dto.response.ApiResponse;
import com.techstore.dto.response.PageResponse;
import com.techstore.dto.response.UserResponse;
import com.techstore.enums.RoleCode;
import com.techstore.security.RequireRole;
import com.techstore.security.RoleAuthorizationInterceptor;
import com.techstore.service.UserManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${app.api.base-path}/admin/users")
@RequireRole(RoleCode.ADMIN)
@Tag(name = "Admin User Management", description = "Admin user account management endpoints")
public class AdminUserController {

    private final UserManagementService userManagementService;

    public AdminUserController(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    @GetMapping
    @Operation(summary = "Get paginated user accounts with optional search keyword")
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> getUsers(
            @RequestParam(name = "keyword", required = false) String keyword,
            @PageableDefault(page = 0, size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PageResponse<UserResponse> response = userManagementService.getUsers(keyword, pageable);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách người dùng thành công", response));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Lock or unlock a user account")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserStatus(
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateUserStatusRequest request,
            HttpServletRequest httpServletRequest
    ) {
        Long currentAdminId = (Long) httpServletRequest.getAttribute(RoleAuthorizationInterceptor.CURRENT_USER_ID_ATTRIBUTE);
        UserResponse response = userManagementService.updateUserStatus(currentAdminId, id, request);
        String actionMessage = "ACTIVE".equalsIgnoreCase(response.status())
                ? "Mở khoá tài khoản thành công"
                : "Khoá tài khoản thành công";
        return ResponseEntity.ok(ApiResponse.success(actionMessage, response));
    }
}
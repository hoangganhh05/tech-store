package com.techstore.service;

import com.techstore.dto.request.UpdateUserStatusRequest;
import com.techstore.dto.response.PageResponse;
import com.techstore.dto.response.UserResponse;
import org.springframework.data.domain.Pageable;

public interface UserManagementService {

    PageResponse<UserResponse> getUsers(String keyword, Pageable pageable);

    UserResponse updateUserStatus(Long currentAdminId, Long targetUserId, UpdateUserStatusRequest request);
}
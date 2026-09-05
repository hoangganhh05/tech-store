package com.techstore.service.impl;

import com.techstore.dto.request.UpdateUserStatusRequest;
import com.techstore.dto.response.PageResponse;
import com.techstore.dto.response.UserResponse;
import com.techstore.entity.User;
import com.techstore.enums.ErrorCode;
import com.techstore.enums.UserStatus;
import com.techstore.exception.BusinessException;
import com.techstore.mapper.UserMapper;
import com.techstore.repository.RefreshTokenRepository;
import com.techstore.repository.UserRepository;
import com.techstore.service.UserManagementService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class UserManagementServiceImpl implements UserManagementService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserMapper userMapper;

    public UserManagementServiceImpl(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            UserMapper userMapper
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> getUsers(String keyword, Pageable pageable) {
        Page<User> usersPage;
        if (keyword != null && !keyword.trim().isEmpty()) {
            usersPage = userRepository.searchUsers(keyword.trim(), pageable);
        } else {
            usersPage = userRepository.findAll(pageable);
        }
        return PageResponse.of(usersPage.map(userMapper::toResponse));
    }

    @Override
    @Transactional
    public UserResponse updateUserStatus(Long currentAdminId, Long targetUserId, UpdateUserStatusRequest request) {
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy người dùng"));

        if (currentAdminId != null && currentAdminId.equals(targetUserId) && request.getStatus() == UserStatus.LOCKED) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Không thể tự khoá tài khoản của chính mình");
        }

        user.changeStatus(request.getStatus());
        User updated = userRepository.save(user);

        if (request.getStatus() == UserStatus.LOCKED) {
            refreshTokenRepository.revokeActiveTokensByUser(user, Instant.now());
        }

        return userMapper.toResponse(updated);
    }
}
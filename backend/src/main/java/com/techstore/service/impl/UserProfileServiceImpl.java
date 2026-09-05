package com.techstore.service.impl;

import com.techstore.dto.request.UpdateProfileRequest;
import com.techstore.dto.response.UserProfileResponse;
import com.techstore.entity.User;
import com.techstore.enums.ErrorCode;
import com.techstore.enums.UserStatus;
import com.techstore.exception.BusinessException;
import com.techstore.repository.UserRepository;
import com.techstore.service.UserProfileService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserProfileServiceImpl implements UserProfileService {

    private final UserRepository userRepository;

    public UserProfileServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(Long userId) {
        return toResponse(findAuthorizedUser(userId));
    }

    @Override
    @Transactional
    public UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = findAuthorizedUser(userId);
        user.updateProfile(request.getFullName(), request.getPhone(), request.getDateOfBirth());
        return toResponse(userRepository.save(user));
    }

    private User findAuthorizedUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.INVALID_ACCESS_TOKEN,
                        "Phiên đăng nhập không hợp lệ hoặc đã hết hạn"
                ));
        if (user.getStatus() == UserStatus.LOCKED) {
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED, "Tài khoản của bạn đã bị khóa. Vui lòng liên hệ hỗ trợ.");
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED, "Tài khoản hiện không thể sử dụng. Vui lòng liên hệ hỗ trợ.");
        }
        return user;
    }

    private UserProfileResponse toResponse(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                user.getDateOfBirth(),
                user.getUpdatedAt()
        );
    }
}

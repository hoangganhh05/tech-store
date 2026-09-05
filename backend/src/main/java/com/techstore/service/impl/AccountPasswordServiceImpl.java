package com.techstore.service.impl;

import com.techstore.dto.request.ChangePasswordRequest;
import com.techstore.entity.User;
import com.techstore.enums.ErrorCode;
import com.techstore.enums.UserStatus;
import com.techstore.exception.BusinessException;
import com.techstore.repository.RefreshTokenRepository;
import com.techstore.repository.UserRepository;
import com.techstore.service.AccountPasswordService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class AccountPasswordServiceImpl implements AccountPasswordService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountPasswordServiceImpl(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = findAuthorizedUser(userId);
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_CURRENT_PASSWORD, "Mật khẩu hiện tại không đúng");
        }
        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.PASSWORD_MUST_BE_DIFFERENT, "Mật khẩu mới phải khác mật khẩu hiện tại");
        }

        user.changePasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        refreshTokenRepository.revokeActiveTokensByUser(user, Instant.now());
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
}

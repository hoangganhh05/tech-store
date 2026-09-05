package com.techstore.service;

import com.techstore.dto.request.ChangePasswordRequest;
import com.techstore.entity.User;
import com.techstore.enums.ErrorCode;
import com.techstore.exception.BusinessException;
import com.techstore.repository.RefreshTokenRepository;
import com.techstore.repository.UserRepository;
import com.techstore.service.impl.AccountPasswordServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountPasswordServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    private AccountPasswordServiceImpl service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new AccountPasswordServiceImpl(userRepository, refreshTokenRepository, passwordEncoder);
        user = new User("customer@example.com", "old-hash", "Nguyen Van A", "0901234567");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    }

    @Test
    void changePasswordVerifiesEncodesAndRevokesActiveSessions() {
        ChangePasswordRequest request = request("old-password", "new-password", "new-password");
        when(passwordEncoder.matches("old-password", "old-hash")).thenReturn(true);
        when(passwordEncoder.matches("new-password", "old-hash")).thenReturn(false);
        when(passwordEncoder.encode("new-password")).thenReturn("new-hash");

        service.changePassword(1L, request);

        assertThat(user.getPasswordHash()).isEqualTo("new-hash");
        verify(userRepository).save(user);
        verify(refreshTokenRepository).revokeActiveTokensByUser(any(User.class), any(Instant.class));
    }

    @Test
    void changePasswordRejectsAnIncorrectCurrentPassword() {
        when(passwordEncoder.matches("wrong-password", "old-hash")).thenReturn(false);

        BusinessException exception = catchThrowableOfType(
                () -> service.changePassword(1L, request("wrong-password", "new-password", "new-password")),
                BusinessException.class
        );

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_CURRENT_PASSWORD);
        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void changePasswordRejectsReusingTheCurrentPassword() {
        when(passwordEncoder.matches("old-password", "old-hash")).thenReturn(true);

        BusinessException exception = catchThrowableOfType(
                () -> service.changePassword(1L, request("old-password", "old-password", "old-password")),
                BusinessException.class
        );

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PASSWORD_MUST_BE_DIFFERENT);
        verify(passwordEncoder, never()).encode(any());
    }

    private ChangePasswordRequest request(String currentPassword, String newPassword, String confirmPassword) {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword(currentPassword);
        request.setNewPassword(newPassword);
        request.setConfirmPassword(confirmPassword);
        return request;
    }
}

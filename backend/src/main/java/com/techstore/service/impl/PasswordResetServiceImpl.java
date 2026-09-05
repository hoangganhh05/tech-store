package com.techstore.service.impl;

import com.techstore.dto.request.ForgotPasswordRequest;
import com.techstore.dto.request.ResetPasswordRequest;
import com.techstore.entity.PasswordResetToken;
import com.techstore.entity.User;
import com.techstore.enums.ErrorCode;
import com.techstore.exception.BusinessException;
import com.techstore.infrastructure.mail.PasswordResetEmailSender;
import com.techstore.repository.PasswordResetTokenRepository;
import com.techstore.repository.RefreshTokenRepository;
import com.techstore.repository.UserRepository;
import com.techstore.security.PasswordResetProperties;
import com.techstore.security.PasswordResetTokenGenerator;
import com.techstore.service.PasswordResetService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.util.Locale;

@Service
public class PasswordResetServiceImpl implements PasswordResetService {

    private static final String INVALID_TOKEN_MESSAGE = "Liên kết đặt lại mật khẩu không hợp lệ, đã được sử dụng hoặc đã hết hạn";

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetTokenGenerator tokenGenerator;
    private final PasswordResetProperties properties;
    private final PasswordResetEmailSender passwordResetEmailSender;

    public PasswordResetServiceImpl(
            UserRepository userRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            PasswordResetTokenGenerator tokenGenerator,
            PasswordResetProperties properties,
            PasswordResetEmailSender passwordResetEmailSender
    ) {
        this.userRepository = userRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenGenerator = tokenGenerator;
        this.properties = properties;
        this.passwordResetEmailSender = passwordResetEmailSender;
    }

    @PostConstruct
    void validateConfiguration() {
        if (properties.getTokenTtl() == null || properties.getTokenTtl().isZero() || properties.getTokenTtl().isNegative()) {
            throw new IllegalStateException("PASSWORD_RESET_TOKEN_TTL must be greater than zero");
        }
        if (properties.getFrontendBaseUrl() == null || properties.getFrontendBaseUrl().isBlank()) {
            throw new IllegalStateException("PASSWORD_RESET_FRONTEND_URL must be configured");
        }
    }

    @Override
    @Transactional
    public void requestPasswordReset(ForgotPasswordRequest request) {
        userRepository.findByEmailIgnoreCase(normalizeEmail(request.getEmail())).ifPresent(user -> {
            Instant now = Instant.now();
            String rawToken = tokenGenerator.generate();
            Instant expiresAt = now.plus(properties.getTokenTtl());

            passwordResetTokenRepository.markUnusedTokensAsUsed(user, now);
            passwordResetTokenRepository.save(new PasswordResetToken(user, tokenGenerator.hash(rawToken), expiresAt));
            passwordResetEmailSender.send(user.getEmail(), buildResetUrl(rawToken), expiresAt);
        });
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String tokenHash = tokenGenerator.hash(request.getToken());
        PasswordResetToken passwordResetToken = passwordResetTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(this::invalidResetToken);
        Instant now = Instant.now();
        if (!passwordResetToken.isUsableAt(now)) {
            throw invalidResetToken();
        }

        User user = passwordResetToken.getUser();

        if (passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.PASSWORD_MUST_BE_DIFFERENT, "Mật khẩu mới phải khác mật khẩu cũ");
        }
        if (passwordResetTokenRepository.claimUsableToken(tokenHash, now, now) != 1) {
            throw invalidResetToken();
        }

        user.changePasswordHash(passwordEncoder.encode(request.getPassword()));
        userRepository.save(user);
        passwordResetTokenRepository.markUnusedTokensAsUsed(user, now);
        refreshTokenRepository.revokeActiveTokensByUser(user, now);
    }

    private BusinessException invalidResetToken() {
        return new BusinessException(ErrorCode.INVALID_PASSWORD_RESET_TOKEN, INVALID_TOKEN_MESSAGE);
    }

    private String buildResetUrl(String rawToken) {
        String frontendBaseUrl = properties.getFrontendBaseUrl().replaceAll("/+$", "");
        return UriComponentsBuilder.fromUriString(frontendBaseUrl)
                .path("/reset-password")
                .queryParam("token", rawToken)
                .build()
                .encode()
                .toUriString();
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}

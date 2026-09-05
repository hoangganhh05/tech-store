package com.techstore.service;

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
import com.techstore.service.impl.PasswordResetServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private PasswordResetTokenGenerator tokenGenerator;

    @Mock
    private PasswordResetEmailSender passwordResetEmailSender;

    private PasswordResetService passwordResetService;

    @BeforeEach
    void setUp() {
        PasswordResetProperties properties = new PasswordResetProperties();
        properties.setTokenTtl(Duration.ofMinutes(30));
        properties.setFrontendBaseUrl("https://app.example.test/");
        passwordResetService = new PasswordResetServiceImpl(
                userRepository,
                passwordResetTokenRepository,
                refreshTokenRepository,
                passwordEncoder,
                tokenGenerator,
                properties,
                passwordResetEmailSender
        );
    }

    @Test
    void requestPasswordResetNormalizesEmailStoresOnlyTheHashAndSendsTheRawTokenInTheLink() {
        User user = user("customer@example.com", "old-password-hash");
        String rawToken = "raw-reset-token";
        String tokenHash = "a".repeat(64);
        when(userRepository.findByEmailIgnoreCase("customer@example.com")).thenReturn(Optional.of(user));
        when(tokenGenerator.generate()).thenReturn(rawToken);
        when(tokenGenerator.hash(rawToken)).thenReturn(tokenHash);

        passwordResetService.requestPasswordReset(forgotRequest(" Customer@Example.com "));

        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(passwordResetTokenRepository).save(tokenCaptor.capture());
        PasswordResetToken savedToken = tokenCaptor.getValue();
        assertThat(savedToken.getUser()).isSameAs(user);
        assertThat(savedToken.getTokenHash()).isEqualTo(tokenHash).isNotEqualTo(rawToken);
        assertThat(savedToken.getExpiresAt()).isAfter(Instant.now());
        verify(passwordResetTokenRepository).markUnusedTokensAsUsed(eq(user), any(Instant.class));

        ArgumentCaptor<String> resetUrlCaptor = ArgumentCaptor.forClass(String.class);
        verify(passwordResetEmailSender).send(eq("customer@example.com"), resetUrlCaptor.capture(), any(Instant.class));
        assertThat(resetUrlCaptor.getValue())
                .isEqualTo("https://app.example.test/reset-password?token=raw-reset-token");
    }

    @Test
    void requestPasswordResetDoesNothingForAnUnknownEmail() {
        when(userRepository.findByEmailIgnoreCase("unknown@example.com")).thenReturn(Optional.empty());

        passwordResetService.requestPasswordReset(forgotRequest("unknown@example.com"));

        verify(passwordResetTokenRepository, never()).markUnusedTokensAsUsed(any(), any());
        verify(passwordResetTokenRepository, never()).save(any());
        verify(passwordResetEmailSender, never()).send(any(), any(), any());
        verify(tokenGenerator, never()).generate();
    }

    @Test
    void resetPasswordChangesTheHashClaimsTheTokenAndRevokesAllRefreshTokens() {
        User user = user("customer@example.com", "old-password-hash");
        PasswordResetToken token = new PasswordResetToken(
                user,
                "token-hash",
                Instant.now().plus(Duration.ofMinutes(10))
        );
        when(tokenGenerator.hash("raw-reset-token")).thenReturn("token-hash");
        when(passwordResetTokenRepository.findByTokenHash("token-hash")).thenReturn(Optional.of(token));
        when(passwordEncoder.matches("new-strong-password", "old-password-hash")).thenReturn(false);
        when(passwordResetTokenRepository.claimUsableToken(eq("token-hash"), any(Instant.class), any(Instant.class))).thenReturn(1);
        when(passwordEncoder.encode("new-strong-password")).thenReturn("new-password-hash");

        passwordResetService.resetPassword(resetRequest("raw-reset-token", "new-strong-password"));

        assertThat(user.getPasswordHash()).isEqualTo("new-password-hash");
        verify(userRepository).save(user);
        verify(passwordResetTokenRepository).markUnusedTokensAsUsed(eq(user), any(Instant.class));
        verify(refreshTokenRepository).revokeActiveTokensByUser(eq(user), any(Instant.class));
    }

    @Test
    void resetPasswordRejectsAnExpiredTokenWithoutChangingThePassword() {
        User user = user("customer@example.com", "old-password-hash");
        PasswordResetToken token = new PasswordResetToken(
                user,
                "expired-token-hash",
                Instant.now().minus(Duration.ofMinutes(1))
        );
        when(tokenGenerator.hash("expired-token")).thenReturn("expired-token-hash");
        when(passwordResetTokenRepository.findByTokenHash("expired-token-hash")).thenReturn(Optional.of(token));

        BusinessException exception = catchThrowableOfType(
                () -> passwordResetService.resetPassword(resetRequest("expired-token", "new-strong-password")),
                BusinessException.class
        );

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_PASSWORD_RESET_TOKEN);
        assertThat(user.getPasswordHash()).isEqualTo("old-password-hash");
        verify(passwordEncoder, never()).matches(any(), any());
        verify(passwordResetTokenRepository, never()).claimUsableToken(any(), any(), any());
        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any());
        verify(refreshTokenRepository, never()).revokeActiveTokensByUser(any(), any());
    }

    @Test
    void resetPasswordRejectsReusingTheCurrentPasswordBeforeClaimingTheToken() {
        User user = user("customer@example.com", "old-password-hash");
        PasswordResetToken token = new PasswordResetToken(
                user,
                "token-hash",
                Instant.now().plus(Duration.ofMinutes(10))
        );
        when(tokenGenerator.hash("raw-reset-token")).thenReturn("token-hash");
        when(passwordResetTokenRepository.findByTokenHash("token-hash")).thenReturn(Optional.of(token));
        when(passwordEncoder.matches("old-password", "old-password-hash")).thenReturn(true);

        BusinessException exception = catchThrowableOfType(
                () -> passwordResetService.resetPassword(resetRequest("raw-reset-token", "old-password")),
                BusinessException.class
        );

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PASSWORD_MUST_BE_DIFFERENT);
        verify(passwordResetTokenRepository, never()).claimUsableToken(any(), any(), any());
        verify(passwordEncoder, never()).encode(any());
    }

    private ForgotPasswordRequest forgotRequest(String email) {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail(email);
        return request;
    }

    private ResetPasswordRequest resetRequest(String token, String password) {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken(token);
        request.setPassword(password);
        request.setConfirmPassword(password);
        return request;
    }

    private User user(String email, String passwordHash) {
        return new User(email, passwordHash, "Nguyen Van A", "0901234567");
    }
}

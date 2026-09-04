package com.techstore.service;

import com.techstore.dto.request.LoginRequest;
import com.techstore.dto.request.LogoutRequest;
import com.techstore.dto.request.RegisterRequest;
import com.techstore.dto.response.LoginResponse;
import com.techstore.dto.response.UserResponse;
import com.techstore.entity.Role;
import com.techstore.entity.RefreshToken;
import com.techstore.entity.User;
import com.techstore.enums.ErrorCode;
import com.techstore.enums.RoleCode;
import com.techstore.enums.UserStatus;
import com.techstore.exception.BusinessException;
import com.techstore.mapper.UserMapper;
import com.techstore.repository.RoleRepository;
import com.techstore.repository.RefreshTokenRepository;
import com.techstore.repository.UserRepository;
import com.techstore.security.IssuedTokenPair;
import com.techstore.security.InvalidRefreshTokenException;
import com.techstore.security.TokenIssuer;
import com.techstore.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenIssuer tokenIssuer;

    private AuthServiceImpl authService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(
                userRepository,
                roleRepository,
                refreshTokenRepository,
                passwordEncoder,
                new UserMapper(),
                tokenIssuer
        );
    }

    @Test
    void registerNormalizesEmailHashesPasswordAndAssignsCustomerRole() {
        RegisterRequest request = request("  Customer@Example.com ");
        Role customerRole = new Role(RoleCode.CUSTOMER, "Customer");
        User savedUser = new User("customer@example.com", "encoded-password", "Nguyen Van A", "0901234567");
        savedUser.addRole(customerRole);

        when(userRepository.existsByEmailIgnoreCase("customer@example.com")).thenReturn(false);
        when(roleRepository.findByCode(RoleCode.CUSTOMER)).thenReturn(Optional.of(customerRole));
        when(passwordEncoder.encode("strong-password")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        UserResponse actual = authService.register(request);

        assertThat(actual.email()).isEqualTo("customer@example.com");
        assertThat(actual.fullName()).isEqualTo("Nguyen Van A");
        assertThat(actual.roles()).containsExactly("CUSTOMER");
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User user = userCaptor.getValue();
        assertThat(user.getEmail()).isEqualTo("customer@example.com");
        assertThat(user.getPasswordHash()).isEqualTo("encoded-password");
        assertThat(user.getFullName()).isEqualTo("Nguyen Van A");
        assertThat(user.getPhone()).isEqualTo("0901234567");
        assertThat(user.getRoles()).extracting(Role::getCode).containsExactly(RoleCode.CUSTOMER);
    }

    @Test
    void registerRejectsAnExistingEmailBeforeEncodingPassword() {
        when(userRepository.existsByEmailIgnoreCase("customer@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request("customer@example.com")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Email đã được đăng ký");

        verify(passwordEncoder, never()).encode(any());
        verify(roleRepository, never()).findByCode(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void loginReturnsTokensAndSafeUserInformationForValidCredentials() {
        User user = customerUser();
        IssuedTokenPair tokenPair = new IssuedTokenPair(
                "access-token",
                "refresh-token",
                "refresh-token-id",
                Instant.parse("2026-09-04T08:15:00Z"),
                Instant.parse("2026-09-11T08:00:00Z")
        );
        when(userRepository.findByEmailIgnoreCase("customer@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("strong-password", "encoded-password")).thenReturn(true);
        when(tokenIssuer.issue(user)).thenReturn(tokenPair);

        LoginResponse actual = authService.login(loginRequest(" Customer@Example.com ", "strong-password"));

        assertThat(actual.accessToken()).isEqualTo("access-token");
        assertThat(actual.refreshToken()).isEqualTo("refresh-token");
        assertThat(actual.tokenType()).isEqualTo("Bearer");
        assertThat(actual.user().email()).isEqualTo("customer@example.com");
        assertThat(actual.user().roles()).containsExactly("CUSTOMER");
        verify(passwordEncoder).matches("strong-password", "encoded-password");
        verify(tokenIssuer).issue(user);
        ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(tokenCaptor.capture());
        assertThat(tokenCaptor.getValue().getTokenId()).isEqualTo("refresh-token-id");
        assertThat(tokenCaptor.getValue().getExpiresAt()).isEqualTo(Instant.parse("2026-09-11T08:00:00Z"));
    }

    @Test
    void loginUsesTheSameGenericErrorForUnknownEmailAndWrongPassword() {
        User user = customerUser();
        when(userRepository.findByEmailIgnoreCase(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(Optional.empty(), Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "encoded-password")).thenReturn(false);

        BusinessException unknownEmail = catchThrowableOfType(
                () -> authService.login(loginRequest("unknown@example.com", "wrong-password")),
                BusinessException.class
        );
        BusinessException wrongPassword = catchThrowableOfType(
                () -> authService.login(loginRequest("customer@example.com", "wrong-password")),
                BusinessException.class
        );

        assertThat(unknownEmail.getErrorCode()).isEqualTo(ErrorCode.INVALID_CREDENTIALS);
        assertThat(unknownEmail.getMessage()).isEqualTo("Email hoặc mật khẩu không đúng");
        assertThat(wrongPassword.getErrorCode()).isEqualTo(unknownEmail.getErrorCode());
        assertThat(wrongPassword.getMessage()).isEqualTo(unknownEmail.getMessage());
        verify(tokenIssuer, never()).issue(any());
    }

    @Test
    void loginRejectsALockedAccountAfterVerifyingThePassword() {
        User user = customerUser();
        user.changeStatus(UserStatus.LOCKED);
        when(userRepository.findByEmailIgnoreCase("customer@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("strong-password", "encoded-password")).thenReturn(true);

        BusinessException exception = catchThrowableOfType(
                () -> authService.login(loginRequest("customer@example.com", "strong-password")),
                BusinessException.class
        );

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ACCOUNT_LOCKED);
        assertThat(exception.getMessage()).contains("đã bị khóa");
        verify(tokenIssuer, never()).issue(any());
    }

    @Test
    void logoutRevokesTheMatchingRefreshToken() {
        RefreshToken refreshToken = new RefreshToken(
                customerUser(),
                "refresh-token-id",
                Instant.parse("2026-09-11T08:00:00Z")
        );
        when(tokenIssuer.getRefreshTokenId("refresh-token")).thenReturn("refresh-token-id");
        when(refreshTokenRepository.findByTokenId("refresh-token-id")).thenReturn(Optional.of(refreshToken));

        authService.logout(logoutRequest("refresh-token"));

        assertThat(refreshToken.isRevoked()).isTrue();
        verify(refreshTokenRepository).save(refreshToken);
    }

    @Test
    void logoutRejectsAnInvalidRefreshToken() {
        when(tokenIssuer.getRefreshTokenId("invalid-token"))
                .thenThrow(new InvalidRefreshTokenException("Refresh token không hợp lệ"));

        BusinessException exception = catchThrowableOfType(
                () -> authService.logout(logoutRequest("invalid-token")),
                BusinessException.class
        );

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);
        verify(refreshTokenRepository, never()).findByTokenId(any());
    }

    private RegisterRequest request(String email) {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Nguyen Van A");
        request.setEmail(email);
        request.setPhone("0901234567");
        request.setPassword("strong-password");
        request.setConfirmPassword("strong-password");
        return request;
    }

    private LoginRequest loginRequest(String email, String password) {
        LoginRequest request = new LoginRequest();
        request.setEmail(email);
        request.setPassword(password);
        return request;
    }

    private LogoutRequest logoutRequest(String refreshToken) {
        LogoutRequest request = new LogoutRequest();
        request.setRefreshToken(refreshToken);
        return request;
    }

    private User customerUser() {
        User user = new User("customer@example.com", "encoded-password", "Nguyen Van A", "0901234567");
        user.addRole(new Role(RoleCode.CUSTOMER, "Customer"));
        return user;
    }
}

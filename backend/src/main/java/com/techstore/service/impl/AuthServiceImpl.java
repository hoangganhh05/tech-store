package com.techstore.service.impl;

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
import com.techstore.service.AuthService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final TokenIssuer tokenIssuer;

    public AuthServiceImpl(
            UserRepository userRepository,
            RoleRepository roleRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            UserMapper userMapper,
            TokenIssuer tokenIssuer
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
        this.tokenIssuer = tokenIssuer;
    }

    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.getEmail());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS, "Email đã được đăng ký");
        }

        Role customerRole = roleRepository.findByCode(RoleCode.CUSTOMER)
                .orElseGet(() -> roleRepository.save(new Role(RoleCode.CUSTOMER, "Customer")));

        User user = new User(
                email,
                passwordEncoder.encode(request.getPassword()),
                request.getFullName().trim(),
                request.getPhone().trim()
        );
        user.addRole(customerRole);

        return userMapper.toResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(normalizeEmail(request.getEmail()))
                .orElseThrow(this::invalidCredentials);

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw invalidCredentials();
        }
        if (user.getStatus() == UserStatus.LOCKED) {
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED, "Tài khoản của bạn đã bị khóa. Vui lòng liên hệ hỗ trợ.");
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED, "Tài khoản hiện không thể đăng nhập. Vui lòng liên hệ hỗ trợ.");
        }

        IssuedTokenPair tokens = tokenIssuer.issue(user);
        refreshTokenRepository.save(new RefreshToken(user, tokens.refreshTokenId(), tokens.refreshTokenExpiresAt()));
        return new LoginResponse(
                tokens.accessToken(),
                tokens.refreshToken(),
                "Bearer",
                tokens.accessTokenExpiresAt(),
                tokens.refreshTokenExpiresAt(),
                userMapper.toResponse(user)
        );
    }

    @Override
    @Transactional
    public LoginResponse adminLogin(LoginRequest request) {
        LoginResponse response = login(request);
        if (response.user() == null || response.user().roles() == null || !response.user().roles().contains(RoleCode.ADMIN.name())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "Tài khoản không có quyền truy cập khu vực quản trị");
        }
        return response;
    }

    @Override
    @Transactional
    public void logout(LogoutRequest request) {
        String tokenId;
        try {
            tokenId = tokenIssuer.getRefreshTokenId(request.getRefreshToken());
        } catch (InvalidRefreshTokenException exception) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN, "Phiên đăng nhập không hợp lệ hoặc đã hết hạn", exception);
        }

        RefreshToken refreshToken = refreshTokenRepository.findByTokenId(tokenId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.INVALID_REFRESH_TOKEN,
                        "Phiên đăng nhập không hợp lệ hoặc đã hết hạn"
                ));
        refreshToken.revoke();
        refreshTokenRepository.save(refreshToken);
    }

    private BusinessException invalidCredentials() {
        return new BusinessException(ErrorCode.INVALID_CREDENTIALS, "Email hoặc mật khẩu không đúng");
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(java.util.Locale.ROOT);
    }
}

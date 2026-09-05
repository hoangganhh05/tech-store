package com.techstore.controller;

import com.techstore.dto.request.ForgotPasswordRequest;
import com.techstore.dto.request.LoginRequest;
import com.techstore.dto.request.LogoutRequest;
import com.techstore.dto.request.ResetPasswordRequest;
import com.techstore.dto.request.RegisterRequest;
import com.techstore.dto.response.ApiResponse;
import com.techstore.dto.response.LoginResponse;
import com.techstore.dto.response.UserResponse;
import com.techstore.service.AuthService;
import com.techstore.service.PasswordResetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${app.api.base-path}/auth")
@Tag(name = "Authentication", description = "Account registration and authentication endpoints")
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    public AuthController(AuthService authService, PasswordResetService passwordResetService) {
        this.authService = authService;
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a customer account")
    public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Đăng ký tài khoản thành công", response));
    }

    @PostMapping("/login")
    @Operation(summary = "Log in with email and password")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Đăng nhập thành công", response));
    }

    @PostMapping("/admin/login")
    @Operation(summary = "Log in to admin portal with email and password")
    public ResponseEntity<ApiResponse<LoginResponse>> adminLogin(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.adminLogin(request);
        return ResponseEntity.ok(ApiResponse.success("Đăng nhập quản trị thành công", response));
    }

    @PostMapping("/logout")
    @Operation(summary = "Log out and revoke the current refresh token")
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request);
        return ResponseEntity.ok(ApiResponse.success("Đăng xuất thành công", null));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Send a password-reset link when the account exists")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.requestPasswordReset(request);
        return ResponseEntity.ok(ApiResponse.success(
                "Nếu email này thuộc về một tài khoản, chúng tôi đã gửi hướng dẫn đặt lại mật khẩu.",
                null
        ));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset a password with a valid, unused reset token")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Đặt lại mật khẩu thành công", null));
    }
}

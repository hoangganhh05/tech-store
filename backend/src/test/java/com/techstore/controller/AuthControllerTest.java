package com.techstore.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techstore.dto.request.LoginRequest;
import com.techstore.dto.request.RegisterRequest;
import com.techstore.dto.response.LoginResponse;
import com.techstore.dto.response.UserResponse;
import com.techstore.enums.ErrorCode;
import com.techstore.exception.BusinessException;
import com.techstore.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @Test
    void registerReturnsCreatedUserWithoutPasswordHash() throws Exception {
        RegisterRequest request = validRequest();
        UserResponse response = new UserResponse(1L, "customer@example.com", "Nguyen Van A", "0901234567", "ACTIVE", Set.of("CUSTOMER"), false, null);
        when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.email").value("customer@example.com"))
                .andExpect(jsonPath("$.data.roles[0]").value("CUSTOMER"))
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());

        verify(authService).register(any(RegisterRequest.class));
    }

    @Test
    void registerReturnsFieldErrorsForInvalidPayload() throws Exception {
        RegisterRequest request = validRequest();
        request.setFullName("");
        request.setEmail("invalid-email");
        request.setPassword("short");
        request.setConfirmPassword("different");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("fullName"),
                        org.hamcrest.Matchers.containsString("email"),
                        org.hamcrest.Matchers.containsString("password"),
                        org.hamcrest.Matchers.containsString("confirmPassword"))));
    }

    @Test
    void loginReturnsTokensAndUserInformationWithoutAPassword() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("customer@example.com");
        request.setPassword("strong-password");
        UserResponse user = new UserResponse(1L, "customer@example.com", "Nguyen Van A", "0901234567", "ACTIVE", Set.of("CUSTOMER"), false, null);
        LoginResponse response = new LoginResponse("access-token", "refresh-token", "Bearer", null, null, user);
        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.user.email").value("customer@example.com"))
                .andExpect(jsonPath("$.data.user.passwordHash").doesNotExist());
    }

    @Test
    void loginReturnsAGenericUnauthorizedErrorForInvalidCredentials() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("customer@example.com");
        request.setPassword("wrong-password");
        when(authService.login(any(LoginRequest.class))).thenThrow(
                new BusinessException(ErrorCode.INVALID_CREDENTIALS, "Email hoặc mật khẩu không đúng")
        );

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("Email hoặc mật khẩu không đúng"));
    }

    @Test
    void loginReturnsLockedStatusForALockedAccount() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("customer@example.com");
        request.setPassword("strong-password");
        when(authService.login(any(LoginRequest.class))).thenThrow(
                new BusinessException(ErrorCode.ACCOUNT_LOCKED, "Tài khoản của bạn đã bị khóa. Vui lòng liên hệ hỗ trợ.")
        );

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.code").value("ACCOUNT_LOCKED"));
    }

    private RegisterRequest validRequest() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Nguyen Van A");
        request.setEmail("customer@example.com");
        request.setPhone("0901234567");
        request.setPassword("strong-password");
        request.setConfirmPassword("strong-password");
        return request;
    }
}

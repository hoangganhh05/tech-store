package com.techstore.service;

import com.techstore.dto.request.LoginRequest;
import com.techstore.dto.request.LogoutRequest;
import com.techstore.dto.request.RegisterRequest;
import com.techstore.dto.response.LoginResponse;
import com.techstore.dto.response.UserResponse;

public interface AuthService {

    UserResponse register(RegisterRequest request);

    LoginResponse login(LoginRequest request);

    void logout(LogoutRequest request);
}

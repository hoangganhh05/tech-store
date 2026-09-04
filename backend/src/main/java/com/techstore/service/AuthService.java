package com.techstore.service;

import com.techstore.dto.request.RegisterRequest;
import com.techstore.dto.response.UserResponse;

public interface AuthService {

    UserResponse register(RegisterRequest request);
}

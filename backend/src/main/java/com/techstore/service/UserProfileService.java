package com.techstore.service;

import com.techstore.dto.request.UpdateProfileRequest;
import com.techstore.dto.response.UserProfileResponse;

public interface UserProfileService {

    UserProfileResponse getProfile(Long userId);

    UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request);
}

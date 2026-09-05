package com.techstore.service;

import com.techstore.dto.request.ChangePasswordRequest;

public interface AccountPasswordService {

    void changePassword(Long userId, ChangePasswordRequest request);
}

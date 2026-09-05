package com.techstore.service;

import com.techstore.dto.request.ForgotPasswordRequest;
import com.techstore.dto.request.ResetPasswordRequest;

public interface PasswordResetService {

    void requestPasswordReset(ForgotPasswordRequest request);

    void resetPassword(ResetPasswordRequest request);
}

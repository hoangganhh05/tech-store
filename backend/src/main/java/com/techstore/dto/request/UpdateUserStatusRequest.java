package com.techstore.dto.request;

import com.techstore.enums.UserStatus;
import jakarta.validation.constraints.NotNull;

public class UpdateUserStatusRequest {

    @NotNull(message = "Trạng thái không được để trống")
    private UserStatus status;

    public UpdateUserStatusRequest() {
    }

    public UpdateUserStatusRequest(UserStatus status) {
        this.status = status;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }
}
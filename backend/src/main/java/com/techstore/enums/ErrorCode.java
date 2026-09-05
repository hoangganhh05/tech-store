package com.techstore.enums;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED),
    INVALID_CURRENT_PASSWORD(HttpStatus.BAD_REQUEST),
    INVALID_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED),
    INVALID_PASSWORD_RESET_TOKEN(HttpStatus.BAD_REQUEST),
    PASSWORD_MUST_BE_DIFFERENT(HttpStatus.BAD_REQUEST),
    ACCOUNT_LOCKED(HttpStatus.LOCKED),
    ACCOUNT_DISABLED(HttpStatus.FORBIDDEN),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND),
    DATABASE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

    private final HttpStatus status;

    ErrorCode(HttpStatus status) {
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }
}

package com.techstore.exception;

import com.techstore.enums.ErrorCode;

public class DatabaseUnavailableException extends BusinessException {

    public DatabaseUnavailableException(String message) {
        super(ErrorCode.DATABASE_UNAVAILABLE, message);
    }

    public DatabaseUnavailableException(String message, Throwable cause) {
        super(ErrorCode.DATABASE_UNAVAILABLE, message, cause);
    }
}

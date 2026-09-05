package com.techstore.security;

public class InvalidAccessTokenException extends RuntimeException {

    public InvalidAccessTokenException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidAccessTokenException(String message) {
        super(message);
    }
}

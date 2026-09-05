package com.techstore.security;

import com.techstore.enums.ErrorCode;
import com.techstore.exception.BusinessException;
import org.springframework.stereotype.Component;

@Component
public class AccessTokenAuthenticator {

    private static final String INVALID_SESSION_MESSAGE = "Phiên đăng nhập không hợp lệ hoặc đã hết hạn";

    private final TokenIssuer tokenIssuer;

    public AccessTokenAuthenticator(TokenIssuer tokenIssuer) {
        this.tokenIssuer = tokenIssuer;
    }

    public Long authenticate(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            throw invalidAccessToken(null);
        }

        String[] authorizationParts = authorizationHeader.trim().split("\\s+", 2);
        if (authorizationParts.length != 2
                || !"Bearer".equalsIgnoreCase(authorizationParts[0])
                || authorizationParts[1].isBlank()) {
            throw invalidAccessToken(null);
        }

        try {
            return tokenIssuer.getAccessTokenUserId(authorizationParts[1]);
        } catch (InvalidAccessTokenException exception) {
            throw invalidAccessToken(exception);
        }
    }

    private BusinessException invalidAccessToken(Throwable cause) {
        return cause == null
                ? new BusinessException(ErrorCode.INVALID_ACCESS_TOKEN, INVALID_SESSION_MESSAGE)
                : new BusinessException(ErrorCode.INVALID_ACCESS_TOKEN, INVALID_SESSION_MESSAGE, cause);
    }
}

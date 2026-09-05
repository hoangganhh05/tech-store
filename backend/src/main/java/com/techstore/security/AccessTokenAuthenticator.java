package com.techstore.security;

import com.techstore.enums.ErrorCode;
import com.techstore.enums.RoleCode;
import com.techstore.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class AccessTokenAuthenticator {

    private static final String INVALID_SESSION_MESSAGE = "Phiên đăng nhập không hợp lệ hoặc đã hết hạn";
    private static final String ACCESS_DENIED_MESSAGE = "Bạn không có quyền thực hiện thao tác này";

    private final TokenIssuer tokenIssuer;

    public AccessTokenAuthenticator(TokenIssuer tokenIssuer) {
        this.tokenIssuer = tokenIssuer;
    }

    public Long authenticate(String authorizationHeader) {
        return authenticateClaims(authorizationHeader).userId();
    }

    public AccessTokenClaims authenticateClaims(String authorizationHeader) {
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
            return tokenIssuer.getAccessTokenClaims(authorizationParts[1]);
        } catch (InvalidAccessTokenException exception) {
            throw invalidAccessToken(exception);
        }
    }

    public AccessTokenClaims requireRole(String authorizationHeader, RoleCode requiredRole) {
        AccessTokenClaims claims = authenticateClaims(authorizationHeader);
        if (requiredRole != null && !claims.hasRole(requiredRole)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, ACCESS_DENIED_MESSAGE);
        }
        return claims;
    }

    public AccessTokenClaims requireAnyRole(String authorizationHeader, Set<RoleCode> requiredRoles) {
        AccessTokenClaims claims = authenticateClaims(authorizationHeader);
        if (requiredRoles != null && !requiredRoles.isEmpty()) {
            boolean hasAny = requiredRoles.stream().anyMatch(claims::hasRole);
            if (!hasAny) {
                throw new BusinessException(ErrorCode.ACCESS_DENIED, ACCESS_DENIED_MESSAGE);
            }
        }
        return claims;
    }

    private BusinessException invalidAccessToken(Throwable cause) {
        return cause == null
                ? new BusinessException(ErrorCode.INVALID_ACCESS_TOKEN, INVALID_SESSION_MESSAGE)
                : new BusinessException(ErrorCode.INVALID_ACCESS_TOKEN, INVALID_SESSION_MESSAGE, cause);
    }
}

package com.techstore.security;

import com.techstore.enums.RoleCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RoleAuthorizationInterceptor implements HandlerInterceptor {

    public static final String CURRENT_USER_CLAIMS_ATTRIBUTE = "currentUserClaims";
    public static final String CURRENT_USER_ID_ATTRIBUTE = "currentUserId";

    private final ObjectProvider<AccessTokenAuthenticator> accessTokenAuthenticatorProvider;

    public RoleAuthorizationInterceptor(ObjectProvider<AccessTokenAuthenticator> accessTokenAuthenticatorProvider) {
        this.accessTokenAuthenticatorProvider = accessTokenAuthenticatorProvider;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RequireRole methodAnnotation = handlerMethod.getMethodAnnotation(RequireRole.class);
        RequireRole classAnnotation = handlerMethod.getBeanType().getAnnotation(RequireRole.class);
        RequireRole requireRole = methodAnnotation != null ? methodAnnotation : classAnnotation;

        boolean isAdminPath = request.getRequestURI().contains("/admin");
        RoleCode requiredRole = requireRole != null ? requireRole.value() : (isAdminPath ? RoleCode.ADMIN : null);

        if (requiredRole != null) {
            AccessTokenAuthenticator authenticator = accessTokenAuthenticatorProvider.getIfAvailable();
            if (authenticator != null) {
                String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
                AccessTokenClaims claims = authenticator.requireRole(authorizationHeader, requiredRole);
                request.setAttribute(CURRENT_USER_CLAIMS_ATTRIBUTE, claims);
                request.setAttribute(CURRENT_USER_ID_ATTRIBUTE, claims.userId());
            }
        }

        return true;
    }
}

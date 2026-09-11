package com.techstore.security;

import com.techstore.enums.RoleCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.http.HttpStatus;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

@Component
public class RoleAuthorizationInterceptor implements HandlerInterceptor {

    public static final String CURRENT_USER_CLAIMS_ATTRIBUTE = "currentUserClaims";
    public static final String CURRENT_USER_ID_ATTRIBUTE = "currentUserId";

    private final ObjectProvider<AccessTokenAuthenticator> accessTokenAuthenticatorProvider;

    public RoleAuthorizationInterceptor(ObjectProvider<AccessTokenAuthenticator> accessTokenAuthenticatorProvider) {
        this.accessTokenAuthenticatorProvider = accessTokenAuthenticatorProvider;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RequireRole methodAnnotation = handlerMethod.getMethodAnnotation(RequireRole.class);
        RequireRole classAnnotation = handlerMethod.getBeanType().getAnnotation(RequireRole.class);
        RequireRole requireRole = methodAnnotation != null ? methodAnnotation : classAnnotation;

        String path = request.getRequestURI();
        boolean isAdminPath = path.matches(".*\\/admin(?:\\/|$).*") && !path.contains("/auth/");
        Set<RoleCode> requiredRoles = requireRole != null
                ? EnumSet.copyOf(Arrays.asList(requireRole.value()))
                : (isAdminPath ? Set.of(RoleCode.ADMIN) : Set.of());

        if (!requiredRoles.isEmpty()) {
            AccessTokenAuthenticator authenticator = accessTokenAuthenticatorProvider.getIfAvailable();
            if (authenticator == null) {
                // A missing authenticator must never turn a protected endpoint
                // into an anonymous endpoint (fail closed).
                response.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value());
                return false;
            }
            String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
            AccessTokenClaims claims = authenticator.requireAnyRole(authorizationHeader, requiredRoles);
            request.setAttribute(CURRENT_USER_CLAIMS_ATTRIBUTE, claims);
            request.setAttribute(CURRENT_USER_ID_ATTRIBUTE, claims.userId());
        }

        return true;
    }
}

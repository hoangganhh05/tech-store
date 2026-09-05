package com.techstore.security;

import com.techstore.enums.RoleCode;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

public record AccessTokenClaims(
        Long userId,
        String email,
        Set<RoleCode> roles
) {
    public AccessTokenClaims {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(email, "email must not be null");
        roles = roles == null ? Collections.emptySet() : Collections.unmodifiableSet(roles);
    }

    public boolean hasRole(RoleCode role) {
        return roles.contains(role);
    }

    public boolean isAdmin() {
        return hasRole(RoleCode.ADMIN);
    }
}

package com.techstore.security;

import com.techstore.enums.ErrorCode;
import com.techstore.exception.BusinessException;

import java.net.URI;

/**
 * Validates image URLs before they are persisted and later rendered by the
 * storefront.  Only HTTPS/HTTP URLs and single-slash relative paths are
 * accepted; javascript:, data:, protocol-relative and other schemes are not.
 */
public final class SafeUrlValidator {

    private SafeUrlValidator() {
    }

    public static String normalizeImageUrl(String rawUrl) {
        if (rawUrl == null) {
            return null;
        }

        String value = rawUrl.trim();
        if (value.isEmpty()) {
            return null;
        }

        if (value.length() > 255
                || value.indexOf('<') >= 0
                || value.indexOf('>') >= 0
                || value.indexOf('\\') >= 0
                || value.chars().anyMatch(Character::isISOControl)) {
            throw invalidUrl();
        }

        // Generated local uploads are rooted at /uploads; reject //host/path
        // so that it cannot be interpreted as a protocol-relative URL.
        if (value.startsWith("/")) {
            if (!value.startsWith("//")) {
                return value;
            }
            throw invalidUrl();
        }

        try {
            URI uri = URI.create(value);
            String scheme = uri.getScheme();
            if (scheme == null
                    || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))
                    || uri.getHost() == null
                    || uri.getUserInfo() != null) {
                throw invalidUrl();
            }
            return value;
        } catch (IllegalArgumentException exception) {
            throw invalidUrl();
        }
    }

    private static BusinessException invalidUrl() {
        return new BusinessException(ErrorCode.VALIDATION_ERROR, "URL hình ảnh không hợp lệ");
    }
}

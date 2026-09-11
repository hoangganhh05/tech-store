package com.techstore.security;

import com.techstore.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SafeUrlValidatorTest {

    @Test
    void acceptsHttpHttpsAndLocalUploadUrls() {
        assertThat(SafeUrlValidator.normalizeImageUrl(" https://cdn.example.com/image.png "))
                .isEqualTo("https://cdn.example.com/image.png");
        assertThat(SafeUrlValidator.normalizeImageUrl("http://cdn.example.com/image.png"))
                .isEqualTo("http://cdn.example.com/image.png");
        assertThat(SafeUrlValidator.normalizeImageUrl("/uploads/products/image.png"))
                .isEqualTo("/uploads/products/image.png");
        assertThat(SafeUrlValidator.normalizeImageUrl("  ")).isNull();
    }

    @Test
    void rejectsScriptDataAndProtocolRelativeUrls() {
        assertThatThrownBy(() -> SafeUrlValidator.normalizeImageUrl("javascript:alert(1)"))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> SafeUrlValidator.normalizeImageUrl("data:text/html,<svg onload=alert(1)>"))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> SafeUrlValidator.normalizeImageUrl("//attacker.example/image.png"))
                .isInstanceOf(BusinessException.class);
    }
}

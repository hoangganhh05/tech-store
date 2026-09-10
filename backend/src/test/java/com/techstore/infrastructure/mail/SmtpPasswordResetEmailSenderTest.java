package com.techstore.infrastructure.mail;

import com.techstore.config.StoreBrandProperties;
import com.techstore.security.PasswordResetProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SmtpPasswordResetEmailSenderTest {

    private final JavaMailSender javaMailSender = mock(JavaMailSender.class);
    @SuppressWarnings("unchecked")
    private final ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);

    @Test
    void sendsResetEmailWithConfiguredBrandAndContactDetails() {
        when(provider.getIfAvailable()).thenReturn(javaMailSender);

        PasswordResetProperties mailProperties = new PasswordResetProperties();
        mailProperties.setEmailFrom("no-reply@example.test");
        SmtpPasswordResetEmailSender sender = new SmtpPasswordResetEmailSender(
                provider,
                mailProperties,
                new StoreBrandProperties(),
                "smtp.test",
                mock(Environment.class));

        sender.send("customer@example.com", "https://example.test/reset?token=abc", Instant.parse("2026-09-10T10:00:00Z"));

        var captor = org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMailSender).send(captor.capture());
        SimpleMailMessage message = captor.getValue();
        assertThat(message.getTo()).containsExactly("customer@example.com");
        assertThat(message.getSubject()).isEqualTo("Đặt lại mật khẩu Đăng Tùng Mobile");
        assertThat(message.getText()).contains(
                "https://example.test/reset?token=abc",
                "Đăng Tùng Mobile",
                "0867116863",
                "hoanghd064@gmail.com");
    }
}

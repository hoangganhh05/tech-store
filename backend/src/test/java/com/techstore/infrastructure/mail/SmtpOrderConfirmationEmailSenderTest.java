package com.techstore.infrastructure.mail;

import com.techstore.dto.response.PlacedOrderItemResponse;
import com.techstore.event.OrderPlacedEvent;
import com.techstore.config.StoreBrandProperties;
import com.techstore.security.PasswordResetProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SmtpOrderConfirmationEmailSenderTest {

    private final JavaMailSender javaMailSender = mock(JavaMailSender.class);
    @SuppressWarnings("unchecked")
    private final ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
    private final PasswordResetProperties properties = new PasswordResetProperties();
    private final StoreBrandProperties brand = new StoreBrandProperties();
    private final OrderPlacedEvent event = new OrderPlacedEvent(
            "customer@example.com", "Nguyễn Văn A", "TS-ABC123", new BigDecimal("230000"),
            Instant.parse("2026-09-08T03:00:00Z"), "1-2 ngày làm việc",
            List.of(new PlacedOrderItemResponse("Phone", "Black / 128GB", new BigDecimal("100000"), 2, new BigDecimal("200000")))
    );

    @Test
    void sendsRecipientSubjectAndOrderSummary() {
        when(provider.getIfAvailable()).thenReturn(javaMailSender);
        properties.setEmailFrom("no-reply@techstore.test");
        SmtpOrderConfirmationEmailSender sender = new SmtpOrderConfirmationEmailSender(provider, properties, brand, "smtp.test");

        sender.send(event);

        var message = org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMailSender).send(message.capture());
        assertThat(message.getValue().getTo()).containsExactly("customer@example.com");
        assertThat(message.getValue().getSubject()).contains("TS-ABC123");
        assertThat(message.getValue().getSubject()).contains("Đăng Tùng Mobile");
        assertThat(message.getValue().getText()).contains(
                "Phone", "1-2 ngày làm việc", "230000 VND", "Đăng Tùng Mobile", "0867116863", "hoanghd064@gmail.com");
    }

    @Test
    void swallowsMailFailureSoAsyncListenerCanFinishSafely() {
        when(provider.getIfAvailable()).thenReturn(javaMailSender);
        doThrow(new MailSendException("SMTP unavailable")).when(javaMailSender).send(any(SimpleMailMessage.class));
        SmtpOrderConfirmationEmailSender sender = new SmtpOrderConfirmationEmailSender(provider, properties, brand, "smtp.test");

        assertThatCode(() -> sender.send(event)).doesNotThrowAnyException();
    }
}

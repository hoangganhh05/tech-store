package com.techstore.infrastructure.mail;

import com.techstore.event.OrderStatusUpdatedEvent;
import com.techstore.config.StoreBrandProperties;
import com.techstore.security.PasswordResetProperties;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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

class SmtpOrderStatusUpdateEmailSenderTest {

    private final JavaMailSender javaMailSender = mock(JavaMailSender.class);
    @SuppressWarnings("unchecked")
    private final ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
    private final PasswordResetProperties properties = new PasswordResetProperties();
    private final StoreBrandProperties brand = new StoreBrandProperties();

    private OrderStatusUpdatedEvent createEvent(String status, String cancellationReason) {
        return new OrderStatusUpdatedEvent(
                10L,
                "TS-TEST-001",
                "customer@example.com",
                "Nguyễn Văn An",
                status,
                cancellationReason,
                new BigDecimal("650000"),
                "0909123456",
                "123 Nguyễn Trãi, Thanh Xuân, Hà Nội",
                Instant.parse("2026-09-09T10:00:00Z"),
                List.of(
                        new OrderStatusUpdatedEvent.ItemSummary("Bàn phím cơ", "Xanh / Red Switch", 1, new BigDecimal("650000"), new BigDecimal("650000"))
                )
        );
    }

    @Test
    void sendsConfirmedEmailWithCorrectSubjectAndBody() {
        when(provider.getIfAvailable()).thenReturn(javaMailSender);
        properties.setEmailFrom("no-reply@techstore.test");
        SmtpOrderStatusUpdateEmailSender sender = new SmtpOrderStatusUpdateEmailSender(provider, properties, brand, "smtp.test");

        sender.send(createEvent("CONFIRMED", null));

        var captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMailSender).send(captor.capture());
        SimpleMailMessage msg = captor.getValue();
        assertThat(msg.getTo()).containsExactly("customer@example.com");
        assertThat(msg.getSubject()).isEqualTo("[Đăng Tùng Mobile] Đơn hàng TS-TEST-001 đã được xác nhận");
        assertThat(msg.getText()).contains(
                "đã được xác nhận và đang được chuẩn bị đóng gói", "Bàn phím cơ", "650000 VND", "0867116863", "hoanghd064@gmail.com");
    }

    @Test
    void sendsShippingEmailWithCorrectSubjectAndBody() {
        when(provider.getIfAvailable()).thenReturn(javaMailSender);
        SmtpOrderStatusUpdateEmailSender sender = new SmtpOrderStatusUpdateEmailSender(provider, properties, brand, "smtp.test");

        sender.send(createEvent("SHIPPING", null));

        var captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMailSender).send(captor.capture());
        SimpleMailMessage msg = captor.getValue();
        assertThat(msg.getSubject()).isEqualTo("[Đăng Tùng Mobile] Đơn hàng TS-TEST-001 đang được giao");
        assertThat(msg.getText()).contains("đang trên đường giao đến bạn");
    }

    @Test
    void sendsCompletedEmailWithCorrectSubjectAndBody() {
        when(provider.getIfAvailable()).thenReturn(javaMailSender);
        SmtpOrderStatusUpdateEmailSender sender = new SmtpOrderStatusUpdateEmailSender(provider, properties, brand, "smtp.test");

        sender.send(createEvent("COMPLETED", null));

        var captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMailSender).send(captor.capture());
        SimpleMailMessage msg = captor.getValue();
        assertThat(msg.getSubject()).isEqualTo("[Đăng Tùng Mobile] Đơn hàng TS-TEST-001 đã giao thành công");
        assertThat(msg.getText()).contains("đã được giao thành công");
    }

    @Test
    void sendsCancelledEmailWithCancellationReason() {
        when(provider.getIfAvailable()).thenReturn(javaMailSender);
        SmtpOrderStatusUpdateEmailSender sender = new SmtpOrderStatusUpdateEmailSender(provider, properties, brand, "smtp.test");

        sender.send(createEvent("CANCELLED", "Khách hàng đổi ý"));

        var captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMailSender).send(captor.capture());
        SimpleMailMessage msg = captor.getValue();
        assertThat(msg.getSubject()).isEqualTo("[Đăng Tùng Mobile] Đơn hàng TS-TEST-001 đã bị huỷ");
        assertThat(msg.getText()).contains("Đơn hàng của bạn đã bị huỷ", "Lý do huỷ: Khách hàng đổi ý");
    }

    @Test
    void skipsDeliveryWhenSmtpHostNotConfigured() {
        SmtpOrderStatusUpdateEmailSender sender = new SmtpOrderStatusUpdateEmailSender(provider, properties, brand, "");
        sender.send(createEvent("CONFIRMED", null));
        verifyNoInteractions(javaMailSender);
    }

    @Test
    void swallowsMailExceptionGracefully() {
        when(provider.getIfAvailable()).thenReturn(javaMailSender);
        doThrow(new MailSendException("SMTP error")).when(javaMailSender).send(any(SimpleMailMessage.class));
        SmtpOrderStatusUpdateEmailSender sender = new SmtpOrderStatusUpdateEmailSender(provider, properties, brand, "smtp.test");

        assertThatCode(() -> sender.send(createEvent("CONFIRMED", null))).doesNotThrowAnyException();
    }
}

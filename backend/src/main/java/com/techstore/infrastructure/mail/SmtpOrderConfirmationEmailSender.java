package com.techstore.infrastructure.mail;

import com.techstore.event.OrderPlacedEvent;
import com.techstore.config.StoreBrandProperties;
import com.techstore.security.PasswordResetProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Component
public class SmtpOrderConfirmationEmailSender implements OrderConfirmationEmailSender {

    private static final Logger log = LoggerFactory.getLogger(SmtpOrderConfirmationEmailSender.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneOffset.UTC);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final PasswordResetProperties mailProperties;
    private final StoreBrandProperties brand;
    private final String mailHost;

    public SmtpOrderConfirmationEmailSender(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            PasswordResetProperties mailProperties,
            StoreBrandProperties brand,
            @Value("${spring.mail.host:}") String mailHost
    ) {
        this.mailSenderProvider = mailSenderProvider;
        this.mailProperties = mailProperties;
        this.brand = brand;
        this.mailHost = mailHost;
    }

    @Override
    public void send(OrderPlacedEvent event) {
        if (mailHost.isBlank()) {
            log.warn("Order confirmation email was not delivered because SMTP is not configured");
            return;
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.warn("Order confirmation email was not delivered because SMTP is not configured");
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        if (mailProperties.getEmailFrom() != null && !mailProperties.getEmailFrom().isBlank()) {
            message.setFrom(mailProperties.getEmailFrom());
        }
        message.setTo(event.recipientEmail());
        message.setSubject("Xác nhận đặt hàng " + brand.getName() + " - " + event.orderNumber());
        message.setText(buildBody(event));

        try {
            mailSender.send(message);
        } catch (RuntimeException exception) {
            log.error("Order confirmation email delivery failed for order {}", event.orderNumber(), exception);
        }
    }

    private String buildBody(OrderPlacedEvent event) {
        StringBuilder body = new StringBuilder()
                .append("Xin chào ").append(event.recipientName()).append(",\n\n")
                .append("Đơn hàng của bạn đã được ghi nhận thành công.\n")
                .append("Mã đơn hàng: ").append(event.orderNumber()).append("\n")
                .append("Thời gian đặt: ").append(DATE_TIME_FORMATTER.format(event.placedAt())).append(" UTC\n")
                .append("Dự kiến xử lý: ").append(event.estimatedProcessingTime()).append("\n\n")
                .append("Tóm tắt đơn hàng:\n");
        event.items().forEach(item -> body.append("- ")
                .append(item.productName())
                .append(item.variantLabel() == null || item.variantLabel().isBlank() ? "" : " (" + item.variantLabel() + ")")
                .append(" x ").append(item.quantity())
                .append(" = ").append(item.subtotal()).append(" VND\n"));
        return body.append("\nTổng cộng: ").append(event.totalAmount()).append(" VND\n\n")
                .append("Cảm ơn bạn đã mua sắm tại ").append(brand.getName()).append(".\n")
                .append("Liên hệ: ").append(brand.getContactPhone())
                .append(" | ").append(brand.getContactEmail()).append("\n")
                .append(brand.getAddress()).toString();
    }
}

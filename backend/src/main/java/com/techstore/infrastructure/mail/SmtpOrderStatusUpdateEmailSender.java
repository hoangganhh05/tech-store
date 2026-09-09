package com.techstore.infrastructure.mail;

import com.techstore.event.OrderStatusUpdatedEvent;
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
public class SmtpOrderStatusUpdateEmailSender implements OrderStatusUpdateEmailSender {

    private static final Logger log = LoggerFactory.getLogger(SmtpOrderStatusUpdateEmailSender.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneOffset.UTC);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final PasswordResetProperties mailProperties;
    private final String mailHost;

    public SmtpOrderStatusUpdateEmailSender(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            PasswordResetProperties mailProperties,
            @Value("${spring.mail.host:}") String mailHost
    ) {
        this.mailSenderProvider = mailSenderProvider;
        this.mailProperties = mailProperties;
        this.mailHost = mailHost;
    }

    @Override
    public void send(OrderStatusUpdatedEvent event) {
        if (mailHost == null || mailHost.isBlank()) {
            log.warn("Order status email was not delivered because SMTP is not configured");
            return;
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.warn("Order status email was not delivered because SMTP is not configured");
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        if (mailProperties != null && mailProperties.getEmailFrom() != null && !mailProperties.getEmailFrom().isBlank()) {
            message.setFrom(mailProperties.getEmailFrom());
        }
        message.setTo(event.recipientEmail());
        message.setSubject(resolveSubject(event));
        message.setText(buildBody(event));

        try {
            mailSender.send(message);
        } catch (RuntimeException exception) {
            log.error("Order status email delivery failed for order {}", event.orderNumber(), exception);
        }
    }

    String resolveSubject(OrderStatusUpdatedEvent event) {
        return switch (event.status()) {
            case "CONFIRMED" -> "[TechStore] Đơn hàng " + event.orderNumber() + " đã được xác nhận";
            case "SHIPPING" -> "[TechStore] Đơn hàng " + event.orderNumber() + " đang được giao";
            case "COMPLETED" -> "[TechStore] Đơn hàng " + event.orderNumber() + " đã giao thành công";
            case "CANCELLED" -> "[TechStore] Đơn hàng " + event.orderNumber() + " đã bị huỷ";
            default -> "[TechStore] Cập nhật trạng thái đơn hàng " + event.orderNumber();
        };
    }

    String buildBody(OrderStatusUpdatedEvent event) {
        StringBuilder body = new StringBuilder()
                .append("Xin chào ").append(event.recipientName()).append(",\n\n");

        switch (event.status()) {
            case "CONFIRMED" -> body.append("Đơn hàng của bạn đã được xác nhận và đang được chuẩn bị đóng gói.\n");
            case "SHIPPING" -> body.append("Đơn hàng của bạn đã được bàn giao cho đơn vị vận chuyển và đang trên đường giao đến bạn.\n");
            case "COMPLETED" -> body.append("Đơn hàng của bạn đã được giao thành công. Cảm ơn bạn đã mua sắm tại TechStore!\n");
            case "CANCELLED" -> {
                body.append("Đơn hàng của bạn đã bị huỷ.\n");
                if (event.cancellationReason() != null && !event.cancellationReason().isBlank()) {
                    body.append("Lý do huỷ: ").append(event.cancellationReason()).append("\n");
                }
            }
            default -> body.append("Trạng thái đơn hàng của bạn đã được cập nhật thành: ").append(event.status()).append(".\n");
        }

        body.append("\nThông tin đơn hàng:\n")
                .append("- Mã đơn hàng: ").append(event.orderNumber()).append("\n");

        if (event.changedAt() != null) {
            body.append("- Thời gian cập nhật: ").append(DATE_TIME_FORMATTER.format(event.changedAt())).append(" UTC\n");
        }

        if (event.deliveryAddress() != null && !event.deliveryAddress().isBlank()) {
            body.append("- Địa chỉ nhận hàng: ").append(event.deliveryAddress()).append("\n");
        }

        if (event.recipientPhone() != null && !event.recipientPhone().isBlank()) {
            body.append("- Số điện thoại: ").append(event.recipientPhone()).append("\n");
        }

        if (event.items() != null && !event.items().isEmpty()) {
            body.append("\nChi tiết sản phẩm:\n");
            event.items().forEach(item -> body.append("- ")
                    .append(item.productName())
                    .append(item.variantLabel() == null || item.variantLabel().isBlank() ? "" : " (" + item.variantLabel() + ")")
                    .append(" x ").append(item.quantity())
                    .append(" = ").append(item.subtotal()).append(" VND\n"));
        }

        if (event.totalAmount() != null) {
            body.append("\nTổng thanh toán: ").append(event.totalAmount()).append(" VND\n");
        }

        body.append("\nNếu bạn có bất kỳ thắc mắc nào, vui lòng liên hệ với đội ngũ hỗ trợ của chúng tôi.\n\n")
                .append("Trân trọng,\nĐội ngũ TechStore.");

        return body.toString();
    }
}

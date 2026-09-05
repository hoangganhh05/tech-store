package com.techstore.infrastructure.mail;

import com.techstore.security.PasswordResetProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.util.Arrays;

@Component
public class SmtpPasswordResetEmailSender implements PasswordResetEmailSender {

    private static final Logger log = LoggerFactory.getLogger(SmtpPasswordResetEmailSender.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final PasswordResetProperties properties;
    private final String mailHost;
    private final Environment environment;

    public SmtpPasswordResetEmailSender(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            PasswordResetProperties properties,
            @Value("${spring.mail.host:}") String mailHost,
            Environment environment
    ) {
        this.mailSenderProvider = mailSenderProvider;
        this.properties = properties;
        this.mailHost = mailHost;
        this.environment = environment;
    }

    @PostConstruct
    void validateMailConfiguration() {
        boolean productionProfileIsActive = Arrays.stream(environment.getActiveProfiles())
                .anyMatch("prod"::equals);
        if (productionProfileIsActive && mailHost.isBlank()) {
            throw new IllegalStateException("MAIL_HOST must be configured for the prod profile");
        }
    }

    @Override
    public void send(String recipientEmail, String resetUrl, Instant expiresAt) {
        if (mailHost.isBlank()) {
            log.warn("Password reset email was not delivered because SMTP is not configured");
            return;
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.warn("Password reset email was not delivered because SMTP is not configured");
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        if (properties.getEmailFrom() != null && !properties.getEmailFrom().isBlank()) {
            message.setFrom(properties.getEmailFrom());
        }
        message.setTo(recipientEmail);
        message.setSubject("Đặt lại mật khẩu TechStore");
        message.setText("Chúng tôi đã nhận được yêu cầu đặt lại mật khẩu.\n\n"
                + "Mở liên kết sau để tạo mật khẩu mới (có hiệu lực đến " + expiresAt + "):\n"
                + resetUrl + "\n\n"
                + "Nếu bạn không yêu cầu thao tác này, bạn có thể bỏ qua email.");

        try {
            mailSender.send(message);
        } catch (MailException exception) {
            log.error("Password reset email delivery failed");
        }
    }
}

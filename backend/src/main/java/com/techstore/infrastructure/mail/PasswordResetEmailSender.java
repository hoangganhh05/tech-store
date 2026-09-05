package com.techstore.infrastructure.mail;

import java.time.Instant;

public interface PasswordResetEmailSender {

    void send(String recipientEmail, String resetUrl, Instant expiresAt);
}

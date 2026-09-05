package com.techstore.e2e;

import com.techstore.entity.PasswordResetToken;
import com.techstore.entity.RefreshToken;
import com.techstore.entity.User;
import com.techstore.repository.PasswordResetTokenRepository;
import com.techstore.repository.RefreshTokenRepository;
import com.techstore.repository.UserRepository;
import com.techstore.security.PasswordResetTokenGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PasswordResetIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordResetTokenGenerator tokenGenerator;

    @BeforeEach
    void cleanData() {
        passwordResetTokenRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void forgotPasswordReturnsTheSameResponseForKnownAndUnknownEmails() {
        register("customer@example.com", "strong-password");

        ResponseEntity<Map> knownEmail = restTemplate.postForEntity(
                "/api/v1/auth/forgot-password",
                jsonRequest(Map.of("email", " CUSTOMER@example.com ")),
                Map.class
        );
        ResponseEntity<Map> unknownEmail = restTemplate.postForEntity(
                "/api/v1/auth/forgot-password",
                jsonRequest(Map.of("email", "unknown@example.com")),
                Map.class
        );

        assertThat(knownEmail.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(unknownEmail.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(knownEmail.getBody()).containsEntry("success", true);
        assertThat(knownEmail.getBody().get("message")).isEqualTo(unknownEmail.getBody().get("message"));
        assertThat(passwordResetTokenRepository.findAll())
                .hasSize(1)
                .allSatisfy(token -> assertThat(token.getTokenHash()).matches("[0-9a-f]{64}"));
    }

    @Test
    void resetPasswordInvalidatesTheOldPasswordExistingSessionsAndTheUsedToken() {
        String email = "customer@example.com";
        String oldPassword = "strong-password";
        String newPassword = "new-strong-password";
        String rawToken = "valid-reset-token";
        register(email, oldPassword);
        login(email, oldPassword);

        User user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        passwordResetTokenRepository.save(new PasswordResetToken(
                user,
                tokenGenerator.hash(rawToken),
                Instant.now().plus(Duration.ofMinutes(10))
        ));

        ResponseEntity<Map> resetResponse = restTemplate.postForEntity(
                "/api/v1/auth/reset-password",
                resetRequest(rawToken, newPassword),
                Map.class
        );

        assertThat(resetResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resetResponse.getBody()).containsEntry("success", true);
        assertThat(refreshTokenRepository.findAll())
                .isNotEmpty()
                .allSatisfy(RefreshToken::isRevoked);
        assertThat(login(email, oldPassword).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(login(email, newPassword).getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<Map> reusedToken = restTemplate.postForEntity(
                "/api/v1/auth/reset-password",
                resetRequest(rawToken, "another-strong-password"),
                Map.class
        );
        assertThat(reusedToken.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(reusedToken.getBody()).containsEntry("code", "INVALID_PASSWORD_RESET_TOKEN");
    }

    @Test
    void resetPasswordRejectsAnExpiredToken() {
        String email = "customer@example.com";
        String rawToken = "expired-reset-token";
        register(email, "strong-password");
        User user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        passwordResetTokenRepository.save(new PasswordResetToken(
                user,
                tokenGenerator.hash(rawToken),
                Instant.now().minus(Duration.ofMinutes(1))
        ));

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/v1/auth/reset-password",
                resetRequest(rawToken, "new-strong-password"),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("code", "INVALID_PASSWORD_RESET_TOKEN");
        assertThat(login(email, "strong-password").getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private void register(String email, String password) {
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/v1/auth/register",
                jsonRequest(Map.of(
                        "fullName", "Nguyen Van A",
                        "email", email,
                        "phone", "0901234567",
                        "password", password,
                        "confirmPassword", password
                )),
                Map.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    private ResponseEntity<Map> login(String email, String password) {
        return restTemplate.postForEntity(
                "/api/v1/auth/login",
                jsonRequest(Map.of("email", email, "password", password)),
                Map.class
        );
    }

    private HttpEntity<Map<String, String>> resetRequest(String token, String password) {
        return jsonRequest(Map.of(
                "token", token,
                "password", password,
                "confirmPassword", password
        ));
    }

    private HttpEntity<Map<String, String>> jsonRequest(Map<String, String> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }
}

package com.techstore.e2e;

import com.techstore.entity.User;
import com.techstore.repository.PasswordResetTokenRepository;
import com.techstore.repository.UserRepository;
import com.techstore.repository.RefreshTokenRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthRegistrationIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanUsers() {
        passwordResetTokenRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void registrationPersistsEncodedPasswordAndCustomerRole() {
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/v1/auth/register",
                jsonRequest(" New User ", "New.User@Example.com"),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        User user = userRepository.findByEmailIgnoreCase("new.user@example.com").orElseThrow();
        assertThat(user.getFullName()).isEqualTo("New User");
        assertThat(user.getPhone()).isEqualTo("0901234567");
        assertThat(passwordEncoder.matches("strong-password", user.getPasswordHash())).isTrue();
        assertThat(user.getRoleCodes()).containsExactlyInAnyOrder(com.techstore.enums.RoleCode.CUSTOMER);
    }

    @Test
    void registrationRejectsDuplicateEmailWithConflict() {
        restTemplate.postForEntity(
                "/api/v1/auth/register",
                jsonRequest("First User", "same@example.com"),
                Map.class
        );

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/v1/auth/register",
                jsonRequest("Second User", " SAME@example.com "),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).containsEntry("code", "EMAIL_ALREADY_EXISTS");
    }

    private HttpEntity<Map<String, String>> jsonRequest(String fullName, String email) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(Map.of(
                "fullName", fullName,
                "email", email,
                "phone", "0901234567",
                "password", "strong-password",
                "confirmPassword", "strong-password"
        ), headers);
    }
}

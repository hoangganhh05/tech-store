package com.techstore.e2e;

import com.techstore.repository.PasswordResetTokenRepository;
import com.techstore.repository.RefreshTokenRepository;
import com.techstore.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserProfileIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;
    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @BeforeEach
    void cleanUsers() {
        passwordResetTokenRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void authenticatedUserCanViewAndUpdateProfileWithoutChangingEmail() {
        register();
        Map<String, Object> loginData = login();
        String accessToken = (String) loginData.get("accessToken");

        ResponseEntity<Map> initial = restTemplate.exchange(
                "/api/v1/users/me", HttpMethod.GET, authorized(accessToken, null), Map.class
        );
        assertThat(initial.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(data(initial)).containsEntry("email", "customer@example.com");

        Map<String, Object> update = new HashMap<>();
        update.put("fullName", "Nguyen Van B");
        update.put("phone", "0987654321");
        update.put("dateOfBirth", "2000-05-20");
        update.put("email", "attacker@example.com");
        ResponseEntity<Map> updated = restTemplate.exchange(
                "/api/v1/users/me", HttpMethod.PUT, authorized(accessToken, update), Map.class
        );

        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(data(updated))
                .containsEntry("email", "customer@example.com")
                .containsEntry("fullName", "Nguyen Van B")
                .containsEntry("phone", "0987654321")
                .containsEntry("dateOfBirth", "2000-05-20");
        assertThat(userRepository.findByEmailIgnoreCase("customer@example.com").orElseThrow().getFullName())
                .isEqualTo("Nguyen Van B");
        assertThat(userRepository.findByEmailIgnoreCase("attacker@example.com")).isEmpty();
    }

    @Test
    void updateReturnsFieldErrorsForInvalidProfileData() {
        register();
        String accessToken = (String) login().get("accessToken");

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/users/me",
                HttpMethod.PUT,
                authorized(accessToken, Map.of("fullName", "", "phone", "abc", "dateOfBirth", "2999-01-01")),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("code", "VALIDATION_ERROR");
        assertThat((String) response.getBody().get("message"))
                .contains("fullName", "phone", "dateOfBirth");
    }

    @Test
    void profileRequiresAValidAccessToken() {
        ResponseEntity<Map> missing = restTemplate.getForEntity("/api/v1/users/me", Map.class);

        assertThat(missing.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(missing.getBody()).containsEntry("code", "INVALID_ACCESS_TOKEN");

        register();
        String refreshToken = (String) login().get("refreshToken");
        ResponseEntity<Map> wrongTokenType = restTemplate.exchange(
                "/api/v1/users/me", HttpMethod.GET, authorized(refreshToken, null), Map.class
        );
        assertThat(wrongTokenType.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(wrongTokenType.getBody()).containsEntry("code", "INVALID_ACCESS_TOKEN");
    }

    private void register() {
        restTemplate.postForEntity("/api/v1/auth/register", json(Map.of(
                "fullName", "Nguyen Van A",
                "email", "customer@example.com",
                "phone", "0901234567",
                "password", "strong-password",
                "confirmPassword", "strong-password"
        )), Map.class);
    }

    private Map<String, Object> login() {
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/v1/auth/login",
                json(Map.of("email", "customer@example.com", "password", "strong-password")),
                Map.class
        );
        return data(response);
    }

    private HttpEntity<Map<String, Object>> authorized(String accessToken, Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    private HttpEntity<Map<String, Object>> json(Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    private Map<String, Object> data(ResponseEntity<Map> response) {
        return (Map<String, Object>) response.getBody().get("data");
    }
}

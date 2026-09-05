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

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ChangePasswordIntegrationTest {

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
    void authenticatedUserCanChangePasswordAndOldSessionsAreRevoked() {
        register();
        String accessToken = (String) login("old-password").get("accessToken");

        ResponseEntity<Map> response = changePassword(accessToken, Map.of(
                "currentPassword", "old-password",
                "newPassword", "new-password",
                "confirmPassword", "new-password"
        ));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("message", "Đổi mật khẩu thành công");
        assertThat(refreshTokenRepository.findAll()).allMatch(token -> token.getRevokedAt() != null);
        assertThat(loginResponse("old-password").getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(loginResponse("new-password").getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void incorrectCurrentPasswordDoesNotChangeThePassword() {
        register();
        String accessToken = (String) login("old-password").get("accessToken");

        ResponseEntity<Map> response = changePassword(accessToken, Map.of(
                "currentPassword", "wrong-password",
                "newPassword", "new-password",
                "confirmPassword", "new-password"
        ));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("code", "INVALID_CURRENT_PASSWORD");
        assertThat(loginResponse("old-password").getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void currentPasswordCannotBeReusedAndRequestRequiresAuthentication() {
        register();
        String accessToken = (String) login("old-password").get("accessToken");
        Map<String, Object> body = Map.of(
                "currentPassword", "old-password",
                "newPassword", "old-password",
                "confirmPassword", "old-password"
        );

        ResponseEntity<Map> reused = changePassword(accessToken, body);
        ResponseEntity<Map> missingToken = restTemplate.exchange(
                "/api/v1/users/me/password", HttpMethod.PUT, json(body), Map.class
        );

        assertThat(reused.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(reused.getBody()).containsEntry("code", "PASSWORD_MUST_BE_DIFFERENT");
        assertThat(missingToken.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(missingToken.getBody()).containsEntry("code", "INVALID_ACCESS_TOKEN");
    }

    private void register() {
        restTemplate.postForEntity("/api/v1/auth/register", json(Map.of(
                "fullName", "Nguyen Van A",
                "email", "customer@example.com",
                "phone", "0901234567",
                "password", "old-password",
                "confirmPassword", "old-password"
        )), Map.class);
    }

    private Map<String, Object> login(String password) {
        return (Map<String, Object>) loginResponse(password).getBody().get("data");
    }

    private ResponseEntity<Map> loginResponse(String password) {
        return restTemplate.postForEntity(
                "/api/v1/auth/login",
                json(Map.of("email", "customer@example.com", "password", password)),
                Map.class
        );
    }

    private ResponseEntity<Map> changePassword(String accessToken, Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.exchange(
                "/api/v1/users/me/password", HttpMethod.PUT, new HttpEntity<>(body, headers), Map.class
        );
    }

    private HttpEntity<Map<String, Object>> json(Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }
}

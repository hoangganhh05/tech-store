package com.techstore.e2e;

import com.techstore.entity.RefreshToken;
import com.techstore.repository.PasswordResetTokenRepository;
import com.techstore.repository.RefreshTokenRepository;
import com.techstore.repository.UserRepository;
import com.techstore.security.TokenIssuer;
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

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthLogoutIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private TokenIssuer tokenIssuer;

    @BeforeEach
    void cleanData() {
        passwordResetTokenRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void logoutRevokesThePersistedRefreshTokenAndIsIdempotent() {
        register("customer@example.com", "strong-password");
        String refreshToken = login("customer@example.com", "strong-password").get("refreshToken").toString();
        String refreshTokenId = tokenIssuer.getRefreshTokenId(refreshToken);

        RefreshToken persistedToken = refreshTokenRepository.findByTokenId(refreshTokenId).orElseThrow();
        assertThat(persistedToken.isRevoked()).isFalse();

        ResponseEntity<Map> firstLogout = logout(refreshToken);
        ResponseEntity<Map> secondLogout = logout(refreshToken);

        assertThat(firstLogout.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(firstLogout.getBody()).containsEntry("success", true);
        assertThat(secondLogout.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(refreshTokenRepository.findByTokenId(refreshTokenId).orElseThrow().isRevoked()).isTrue();
    }

    @Test
    void logoutRejectsAnAccessTokenInPlaceOfARefreshToken() {
        register("customer@example.com", "strong-password");
        String accessToken = login("customer@example.com", "strong-password").get("accessToken").toString();

        ResponseEntity<Map> response = logout(accessToken);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).containsEntry("code", "INVALID_REFRESH_TOKEN");
    }

    private void register(String email, String password) {
        restTemplate.postForEntity(
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
    }

    private Map<String, Object> login(String email, String password) {
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/v1/auth/login",
                jsonRequest(Map.of("email", email, "password", password)),
                Map.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return data(response);
    }

    private ResponseEntity<Map> logout(String refreshToken) {
        return restTemplate.postForEntity(
                "/api/v1/auth/logout",
                jsonRequest(Map.of("refreshToken", refreshToken)),
                Map.class
        );
    }

    private HttpEntity<Map<String, String>> jsonRequest(Map<String, String> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> data(ResponseEntity<Map> response) {
        return (Map<String, Object>) response.getBody().get("data");
    }
}

package com.techstore.e2e;

import com.techstore.entity.User;
import com.techstore.enums.UserStatus;
import com.techstore.repository.RefreshTokenRepository;
import com.techstore.repository.UserRepository;
import com.techstore.security.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
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

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthLoginIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private JwtProperties jwtProperties;

    @BeforeEach
    void cleanUsers() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void loginIssuesSignedAccessAndRefreshTokensForValidCredentials() {
        register("customer@example.com", "strong-password");

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/v1/auth/login",
                loginRequest(" CUSTOMER@example.com ", "strong-password"),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> data = data(response);
        String accessToken = (String) data.get("accessToken");
        String refreshToken = (String) data.get("refreshToken");
        assertThat(accessToken).isNotBlank();
        assertThat(refreshToken).isNotBlank().isNotEqualTo(accessToken);
        assertThat(data.get("tokenType")).isEqualTo("Bearer");

        Claims accessClaims = parseClaims(accessToken);
        Claims refreshClaims = parseClaims(refreshToken);
        assertThat(accessClaims.getSubject()).isEqualTo("customer@example.com");
        assertThat(accessClaims.get("type", String.class)).isEqualTo("access");
        assertThat(refreshClaims.get("type", String.class)).isEqualTo("refresh");

        Map<String, Object> user = (Map<String, Object>) data.get("user");
        assertThat(user).containsEntry("email", "customer@example.com");
        assertThat(user).doesNotContainKey("passwordHash");
    }

    @Test
    void loginDoesNotRevealWhetherTheEmailExists() {
        register("customer@example.com", "strong-password");

        ResponseEntity<Map> unknownEmail = restTemplate.postForEntity(
                "/api/v1/auth/login", loginRequest("unknown@example.com", "wrong-password"), Map.class
        );
        ResponseEntity<Map> wrongPassword = restTemplate.postForEntity(
                "/api/v1/auth/login", loginRequest("customer@example.com", "wrong-password"), Map.class
        );

        assertThat(unknownEmail.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(wrongPassword.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(unknownEmail.getBody()).containsEntry("code", "INVALID_CREDENTIALS");
        assertThat(wrongPassword.getBody()).containsEntry("code", "INVALID_CREDENTIALS");
        assertThat(wrongPassword.getBody().get("message")).isEqualTo(unknownEmail.getBody().get("message"));
    }

    @Test
    void loginBlocksALockedAccount() {
        register("locked@example.com", "strong-password");
        User lockedUser = userRepository.findByEmailIgnoreCase("locked@example.com").orElseThrow();
        lockedUser.changeStatus(UserStatus.LOCKED);
        userRepository.save(lockedUser);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/v1/auth/login", loginRequest("locked@example.com", "strong-password"), Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.LOCKED);
        assertThat(response.getBody()).containsEntry("code", "ACCOUNT_LOCKED");
    }

    private void register(String email, String password) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        restTemplate.postForEntity(
                "/api/v1/auth/register",
                new HttpEntity<>(Map.of(
                        "fullName", "Nguyen Van A",
                        "email", email,
                        "phone", "0901234567",
                        "password", password,
                        "confirmPassword", password
                ), headers),
                Map.class
        );
    }

    private HttpEntity<Map<String, String>> loginRequest(String email, String password) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(Map.of("email", email, "password", password), headers);
    }

    private Map<String, Object> data(ResponseEntity<Map> response) {
        return (Map<String, Object>) response.getBody().get("data");
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}

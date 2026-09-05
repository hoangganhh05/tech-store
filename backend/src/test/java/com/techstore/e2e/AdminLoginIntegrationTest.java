package com.techstore.e2e;

import com.techstore.entity.Role;
import com.techstore.entity.User;
import com.techstore.enums.RoleCode;
import com.techstore.repository.PasswordResetTokenRepository;
import com.techstore.repository.RefreshTokenRepository;
import com.techstore.repository.RoleRepository;
import com.techstore.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AdminLoginIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        passwordResetTokenRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();

        Role customerRole = roleRepository.findByCode(RoleCode.CUSTOMER)
                .orElseGet(() -> roleRepository.save(new Role(RoleCode.CUSTOMER, "Khách hàng")));
        Role adminRole = roleRepository.findByCode(RoleCode.ADMIN)
                .orElseGet(() -> roleRepository.save(new Role(RoleCode.ADMIN, "Quản trị viên")));

        User customer = new User("customer@test.local", passwordEncoder.encode("Password@123"), "Customer User", "0901234567");
        customer.addRole(customerRole);
        userRepository.save(customer);

        User admin = new User("admin@test.local", passwordEncoder.encode("Admin@123456"), "Admin User", "0912345678");
        admin.addRole(adminRole);
        userRepository.save(admin);
    }

    @Test
    @DisplayName("Admin login succeeds when user has ADMIN role")
    void adminLoginSucceedsWithAdminRole() {
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/v1/auth/admin/login",
                loginRequest("admin@test.local", "Admin@123456"),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("success")).isEqualTo(true);
        assertThat(response.getBody().get("message")).isEqualTo("Đăng nhập quản trị thành công");

        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        assertThat(data.get("accessToken")).isNotNull();
        assertThat(data.get("refreshToken")).isNotNull();

        Map<String, Object> user = (Map<String, Object>) data.get("user");
        assertThat(user.get("email")).isEqualTo("admin@test.local");
        List<String> roles = (List<String>) user.get("roles");
        assertThat(roles).contains("ADMIN");
    }

    @Test
    @DisplayName("Admin login rejects CUSTOMER user with 403 Forbidden")
    void adminLoginFailsWithCustomerRole() {
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/v1/auth/admin/login",
                loginRequest("customer@test.local", "Password@123"),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("success")).isEqualTo(false);
        assertThat(response.getBody().get("code")).isEqualTo("ACCESS_DENIED");
        assertThat(response.getBody().get("message")).isEqualTo("Tài khoản không có quyền truy cập khu vực quản trị");
    }

    @Test
    @DisplayName("Admin login fails with invalid password")
    void adminLoginFailsWithInvalidPassword() {
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/v1/auth/admin/login",
                loginRequest("admin@test.local", "WrongPassword!"),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("success")).isEqualTo(false);
        assertThat(response.getBody().get("code")).isEqualTo("INVALID_CREDENTIALS");
    }

    @Test
    @DisplayName("Admin login fails with nonexistent email")
    void adminLoginFailsWithNonexistentEmail() {
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/v1/auth/admin/login",
                loginRequest("nobody@test.local", "Admin@123456"),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("success")).isEqualTo(false);
        assertThat(response.getBody().get("code")).isEqualTo("INVALID_CREDENTIALS");
    }

    private HttpEntity<Map<String, String>> loginRequest(String email, String password) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(Map.of("email", email, "password", password), headers);
    }
}
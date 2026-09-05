package com.techstore.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techstore.dto.request.UpdateUserStatusRequest;
import com.techstore.entity.RefreshToken;
import com.techstore.entity.Role;
import com.techstore.entity.User;
import com.techstore.enums.RoleCode;
import com.techstore.enums.UserStatus;
import com.techstore.repository.PasswordResetTokenRepository;
import com.techstore.repository.RefreshTokenRepository;
import com.techstore.repository.RoleRepository;
import com.techstore.repository.UserRepository;
import com.techstore.security.IssuedTokenPair;
import com.techstore.security.TokenIssuer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class AdminUserManagementIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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

    @Autowired
    private TokenIssuer tokenIssuer;

    private User adminUser;
    private User customerUser1;
    private User customerUser2;

    @BeforeEach
    void setUp() {
        passwordResetTokenRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();

        Role customerRole = roleRepository.findByCode(RoleCode.CUSTOMER)
                .orElseGet(() -> roleRepository.save(new Role(RoleCode.CUSTOMER, "Khách hàng")));
        Role adminRole = roleRepository.findByCode(RoleCode.ADMIN)
                .orElseGet(() -> roleRepository.save(new Role(RoleCode.ADMIN, "Quản trị viên")));

        adminUser = new User("admin.test@example.com", passwordEncoder.encode("Admin@123456"), "Admin User", "0911111111");
        adminUser.addRole(adminRole);
        adminUser = userRepository.save(adminUser);

        customerUser1 = new User("alice@example.com", passwordEncoder.encode("Pass@123"), "Alice Nguyen", "0922222222");
        customerUser1.addRole(customerRole);
        customerUser1 = userRepository.save(customerUser1);

        customerUser2 = new User("bob@example.com", passwordEncoder.encode("Pass@123"), "Bob Tran", "0933333333");
        customerUser2.addRole(customerRole);
        customerUser2 = userRepository.save(customerUser2);
    }

    @Test
    @DisplayName("Admin retrieves paginated user list successfully")
    void adminGetsUsersList() throws Exception {
        IssuedTokenPair adminTokens = tokenIssuer.issue(adminUser);

        mockMvc.perform(get("/api/v1/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminTokens.accessToken())
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalElements").value(3))
                .andExpect(jsonPath("$.data.items.length()").value(3));
    }

    @Test
    @DisplayName("Admin searches users by keyword matching fullName or email")
    void adminSearchesUsers() throws Exception {
        IssuedTokenPair adminTokens = tokenIssuer.issue(adminUser);

        mockMvc.perform(get("/api/v1/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminTokens.accessToken())
                        .param("keyword", "alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.items[0].email").value("alice@example.com"));

        mockMvc.perform(get("/api/v1/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminTokens.accessToken())
                        .param("keyword", "Tran"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.items[0].fullName").value("Bob Tran"));
    }

    @Test
    @DisplayName("Admin locks a customer account and revokes active refresh tokens")
    void adminLocksCustomerAccount() throws Exception {
        IssuedTokenPair adminTokens = tokenIssuer.issue(adminUser);

        // create an active refresh token for customerUser1
        refreshTokenRepository.save(new RefreshToken(customerUser1, "token-id-1", Instant.now().plusSeconds(3600)));

        UpdateUserStatusRequest request = new UpdateUserStatusRequest(UserStatus.LOCKED);

        mockMvc.perform(patch("/api/v1/admin/users/{id}/status", customerUser1.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminTokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("LOCKED"))
                .andExpect(jsonPath("$.message").value("Khoá tài khoản thành công"));

        User refreshed = userRepository.findById(customerUser1.getId()).orElseThrow();
        assertThat(refreshed.getStatus()).isEqualTo(UserStatus.LOCKED);

        // verify refresh token was revoked
        RefreshToken token = refreshTokenRepository.findByTokenId("token-id-1").orElseThrow();
        assertThat(token.isRevoked()).isTrue();
    }

    @Test
    @DisplayName("Admin unlocks a locked customer account")
    void adminUnlocksCustomerAccount() throws Exception {
        IssuedTokenPair adminTokens = tokenIssuer.issue(adminUser);

        customerUser1.changeStatus(UserStatus.LOCKED);
        userRepository.save(customerUser1);

        UpdateUserStatusRequest request = new UpdateUserStatusRequest(UserStatus.ACTIVE);

        mockMvc.perform(patch("/api/v1/admin/users/{id}/status", customerUser1.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminTokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.message").value("Mở khoá tài khoản thành công"));

        User refreshed = userRepository.findById(customerUser1.getId()).orElseThrow();
        assertThat(refreshed.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    @DisplayName("Admin is prevented from locking their own account")
    void adminCannotSelfLock() throws Exception {
        IssuedTokenPair adminTokens = tokenIssuer.issue(adminUser);

        UpdateUserStatusRequest request = new UpdateUserStatusRequest(UserStatus.LOCKED);

        mockMvc.perform(patch("/api/v1/admin/users/{id}/status", adminUser.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminTokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Không thể tự khoá tài khoản của chính mình"));
    }

    @Test
    @DisplayName("Customer cannot access user management endpoints")
    void customerAccessIsForbidden() throws Exception {
        IssuedTokenPair customerTokens = tokenIssuer.issue(customerUser1);

        mockMvc.perform(get("/api/v1/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerTokens.accessToken()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    @DisplayName("Anonymous access returns 401 Unauthorized")
    void anonymousAccessReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_ACCESS_TOKEN"));
    }
}
package com.techstore.security;

import com.techstore.entity.Role;
import com.techstore.entity.User;
import com.techstore.enums.RoleCode;
import com.techstore.repository.RoleRepository;
import com.techstore.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenIssuer tokenIssuer;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User customerUser;
    private User adminUser;

    @BeforeEach
    void setUp() {
        Role customerRole = roleRepository.findByCode(RoleCode.CUSTOMER)
                .orElseGet(() -> roleRepository.save(new Role(RoleCode.CUSTOMER, "Khách hàng")));
        Role adminRole = roleRepository.findByCode(RoleCode.ADMIN)
                .orElseGet(() -> roleRepository.save(new Role(RoleCode.ADMIN, "Quản trị viên")));

        customerUser = new User(
                "customer.test@example.com",
                passwordEncoder.encode("Password@123"),
                "Nguyễn Văn Khách",
                "0901234567"
        );
        customerUser.addRole(customerRole);
        userRepository.save(customerUser);

        adminUser = new User(
                "admin.test@example.com",
                passwordEncoder.encode("Admin@123456"),
                "Quản Trị Viên",
                "0909999999"
        );
        adminUser.addRole(adminRole);
        userRepository.save(adminUser);
    }

    @Test
    @DisplayName("Truy cập route admin không có token sẽ bị chặn với mã lỗi 401 UNAUTHORIZED")
    void anonymousAccessToAdminRouteReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/probe"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("INVALID_ACCESS_TOKEN"));
    }

    @Test
    @DisplayName("Khách hàng (role CUSTOMER) truy cập route admin sẽ bị chặn với mã lỗi 403 FORBIDDEN")
    void customerAccessToAdminRouteReturns403() throws Exception {
        IssuedTokenPair customerTokens = tokenIssuer.issue(customerUser);

        mockMvc.perform(get("/api/v1/admin/probe")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerTokens.accessToken()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
                .andExpect(jsonPath("$.message").value("Bạn không có quyền thực hiện thao tác này"));
    }

    @Test
    @DisplayName("Quản trị viên (role ADMIN) truy cập route admin thành công với HTTP 200 OK")
    void adminAccessToAdminRouteReturns200() throws Exception {
        IssuedTokenPair adminTokens = tokenIssuer.issue(adminUser);

        mockMvc.perform(get("/api/v1/admin/probe")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminTokens.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.authorized").value(true))
                .andExpect(jsonPath("$.data.role").value("ADMIN"))
                .andExpect(jsonPath("$.data.email").value("admin.test@example.com"));
    }

    @Test
    @DisplayName("BCrypt sinh và xác minh mật khẩu Admin@123456 chính xác")
    void verifyPasswordEncoding() {
        String encoded = passwordEncoder.encode("Admin@123456");
        assertThat(passwordEncoder.matches("Admin@123456", encoded)).isTrue();
        System.out.println("GENERATED_ADMIN_HASH=" + encoded);
    }
}

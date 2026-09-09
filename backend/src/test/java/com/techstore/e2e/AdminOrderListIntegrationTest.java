package com.techstore.e2e;

import com.techstore.entity.Order;
import com.techstore.entity.Role;
import com.techstore.entity.User;
import com.techstore.enums.PaymentMethod;
import com.techstore.enums.RoleCode;
import com.techstore.repository.OrderRepository;
import com.techstore.repository.RoleRepository;
import com.techstore.repository.UserRepository;
import com.techstore.security.TokenIssuer;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminOrderListIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired OrderRepository orders;
    @Autowired RoleRepository roles;
    @Autowired UserRepository users;
    @Autowired TokenIssuer tokenIssuer;
    @Autowired EntityManager entityManager;

    private User customerA;
    private User customerB;
    private String adminToken;
    private String customerToken;

    @BeforeEach
    void setUp() {
        Role customerRole = roles.findByCode(RoleCode.CUSTOMER)
                .orElseGet(() -> roles.save(new Role(RoleCode.CUSTOMER, "Khách hàng")));
        Role adminRole = roles.findByCode(RoleCode.ADMIN)
                .orElseGet(() -> roles.save(new Role(RoleCode.ADMIN, "Quản trị viên")));
        customerA = saveUser("admin-orders-a@example.com", "Nguyễn Văn An", "0901000001", customerRole);
        customerB = saveUser("admin-orders-b@example.com", "Trần Thị Bình", "0902000002", customerRole);
        User admin = saveUser("admin-orders-admin@example.com", "Admin", "0903000003", adminRole);
        adminToken = tokenIssuer.issue(admin).accessToken();
        customerToken = tokenIssuer.issue(customerA).accessToken();
    }

    @Test
    void adminCanViewPaginatedNewestFirstOrderSummaries() throws Exception {
        saveOrder(customerA, "TS-ADMIN-OLD", "PENDING", "2026-01-01T10:00:00Z");
        saveOrder(customerB, "TS-ADMIN-MIDDLE", "CONFIRMED", "2026-01-02T10:00:00Z");
        saveOrder(customerA, "TS-ADMIN-NEW", "COMPLETED", "2026-01-03T10:00:00Z");
        entityManager.clear();

        mockMvc.perform(get("/api/v1/admin/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(2)))
                .andExpect(jsonPath("$.data.items[0].orderNumber").value("TS-ADMIN-NEW"))
                .andExpect(jsonPath("$.data.items[0].customerName").value("Nguyễn Văn An"))
                .andExpect(jsonPath("$.data.items[0].customerPhone").value("0901000001"))
                .andExpect(jsonPath("$.data.items[0].totalAmount").value(130000))
                .andExpect(jsonPath("$.data.items[0].status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.items[1].orderNumber").value("TS-ADMIN-MIDDLE"))
                .andExpect(jsonPath("$.data.totalElements").value(3))
                .andExpect(jsonPath("$.data.totalPages").value(2));
    }

    @Test
    void adminCanSearchAndFilterByStatusAndInclusiveDateRange() throws Exception {
        saveOrder(customerA, "TS-AN-ONE", "PENDING", "2026-02-01T10:00:00Z");
        saveOrder(customerB, "TS-BINH-TWO", "CONFIRMED", "2026-02-02T23:30:00Z");
        saveOrder(customerA, "TS-AN-THREE", "COMPLETED", "2026-02-03T10:00:00Z");
        entityManager.clear();

        mockMvc.perform(get("/api/v1/admin/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .param("search", "0901000001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(2)))
                .andExpect(jsonPath("$.data.items[0].orderNumber").value("TS-AN-THREE"));
        mockMvc.perform(get("/api/v1/admin/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .param("search", "TS-AN-ONE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.items[0].orderNumber").value("TS-AN-ONE"));
        mockMvc.perform(get("/api/v1/admin/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .param("search", "Bình"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.items[0].orderNumber").value("TS-BINH-TWO"));
        mockMvc.perform(get("/api/v1/admin/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .param("status", "completed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.items[0].orderNumber").value("TS-AN-THREE"));
        mockMvc.perform(get("/api/v1/admin/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .param("fromDate", "2026-02-02")
                        .param("toDate", "2026-02-02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.items[0].orderNumber").value("TS-BINH-TWO"));
    }

    @Test
    void validatesFiltersAndRestrictsTheListToAdmins() throws Exception {
        mockMvc.perform(get("/api/v1/admin/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .param("status", "UNKNOWN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        mockMvc.perform(get("/api/v1/admin/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .param("fromDate", "2026-03-02")
                        .param("toDate", "2026-03-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        mockMvc.perform(get("/api/v1/admin/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        mockMvc.perform(get("/api/v1/admin/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .param("size", "51"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        mockMvc.perform(get("/api/v1/admin/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        mockMvc.perform(get("/api/v1/admin/orders"))
                .andExpect(status().isUnauthorized());
    }

    private User saveUser(String email, String fullName, String phone, Role role) {
        User user = new User(email, "hash", fullName, phone);
        user.addRole(role);
        return users.saveAndFlush(user);
    }

    private void saveOrder(User user, String number, String status, String placedAt) {
        Order order = orders.saveAndFlush(new Order(number, user, PaymentMethod.COD,
                new BigDecimal("100000"), BigDecimal.ZERO, new BigDecimal("30000")));
        entityManager.createNativeQuery("UPDATE orders SET status = :status, placed_at = :placedAt WHERE id = :id")
                .setParameter("status", status)
                .setParameter("placedAt", Timestamp.from(Instant.parse(placedAt)))
                .setParameter("id", order.getId())
                .executeUpdate();
    }
}

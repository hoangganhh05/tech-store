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
class OrderHistoryIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired OrderRepository orders;
    @Autowired RoleRepository roles;
    @Autowired UserRepository users;
    @Autowired TokenIssuer tokenIssuer;
    @Autowired EntityManager entityManager;

    private User customer;
    private User otherCustomer;
    private String customerToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        Role customerRole = roles.findByCode(RoleCode.CUSTOMER)
                .orElseGet(() -> roles.save(new Role(RoleCode.CUSTOMER, "Khách hàng")));
        Role adminRole = roles.findByCode(RoleCode.ADMIN)
                .orElseGet(() -> roles.save(new Role(RoleCode.ADMIN, "Quản trị viên")));

        customer = new User("order-history-customer@example.com", "hash", "Customer", "0900000001");
        customer.addRole(customerRole);
        customer = users.saveAndFlush(customer);
        customerToken = tokenIssuer.issue(customer).accessToken();

        otherCustomer = new User("order-history-other@example.com", "hash", "Other customer", "0900000002");
        otherCustomer.addRole(customerRole);
        otherCustomer = users.saveAndFlush(otherCustomer);

        User admin = new User("order-history-admin@example.com", "hash", "Admin", "0900000003");
        admin.addRole(adminRole);
        admin = users.saveAndFlush(admin);
        adminToken = tokenIssuer.issue(admin).accessToken();
    }

    @Test
    void listsOnlyCurrentCustomersOrdersNewestFirstAndPaginates() throws Exception {
        Order oldest = saveOrder(customer, "TS-OLD", "PENDING");
        Order middle = saveOrder(customer, "TS-MIDDLE", "COMPLETED");
        Order newest = saveOrder(customer, "TS-NEW", "CONFIRMED");
        saveOrder(otherCustomer, "TS-OTHER", "CONFIRMED");
        setPlacedAt(oldest, Instant.parse("2026-01-01T10:00:00Z"));
        setPlacedAt(middle, Instant.parse("2026-01-02T10:00:00Z"));
        setPlacedAt(newest, Instant.parse("2026-01-03T10:00:00Z"));
        entityManager.clear();

        mockMvc.perform(get("/api/v1/orders/my-orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken)
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(2)))
                .andExpect(jsonPath("$.data.items[0].orderNumber").value("TS-NEW"))
                .andExpect(jsonPath("$.data.items[0].status").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.items[1].orderNumber").value("TS-MIDDLE"))
                .andExpect(jsonPath("$.data.totalElements").value(3))
                .andExpect(jsonPath("$.data.totalPages").value(2))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(2));
    }

    @Test
    void filtersCurrentCustomersOrdersByStatus() throws Exception {
        saveOrder(customer, "TS-PENDING", "PENDING");
        saveOrder(customer, "TS-DONE", "COMPLETED");
        saveOrder(otherCustomer, "TS-OTHER-DONE", "COMPLETED");
        entityManager.clear();

        mockMvc.perform(get("/api/v1/orders/my-orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken)
                        .param("status", "completed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.items[0].orderNumber").value("TS-DONE"))
                .andExpect(jsonPath("$.data.items[0].status").value("COMPLETED"));
    }

    @Test
    void validatesHistoryParametersAndRequiresCustomerRole() throws Exception {
        mockMvc.perform(get("/api/v1/orders/my-orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken)
                        .param("status", "UNKNOWN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        mockMvc.perform(get("/api/v1/orders/my-orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken)
                        .param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        mockMvc.perform(get("/api/v1/orders/my-orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken)
                        .param("size", "51"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        mockMvc.perform(get("/api/v1/orders/my-orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken)
                        .param("page", "not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        mockMvc.perform(get("/api/v1/orders/my-orders"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/orders/my-orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isForbidden());
    }

    private Order saveOrder(User user, String number, String status) {
        Order order = orders.saveAndFlush(new Order(
                number,
                user,
                PaymentMethod.COD,
                new BigDecimal("100000"),
                BigDecimal.ZERO,
                new BigDecimal("30000")
        ));
        entityManager.createNativeQuery("UPDATE orders SET status = :status WHERE id = :id")
                .setParameter("status", status)
                .setParameter("id", order.getId())
                .executeUpdate();
        return order;
    }

    private void setPlacedAt(Order order, Instant placedAt) {
        entityManager.createNativeQuery("UPDATE orders SET placed_at = :placedAt WHERE id = :id")
                .setParameter("placedAt", Timestamp.from(placedAt))
                .setParameter("id", order.getId())
                .executeUpdate();
    }
}

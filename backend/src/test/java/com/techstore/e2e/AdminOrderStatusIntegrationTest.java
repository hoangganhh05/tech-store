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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminOrderStatusIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired OrderRepository orders;
    @Autowired RoleRepository roles;
    @Autowired UserRepository users;
    @Autowired TokenIssuer tokenIssuer;

    private User customer;
    private User admin;
    private String adminToken;
    private String customerToken;

    @BeforeEach
    void setUp() {
        Role customerRole = roles.findByCode(RoleCode.CUSTOMER)
                .orElseGet(() -> roles.save(new Role(RoleCode.CUSTOMER, "Khách hàng")));
        Role adminRole = roles.findByCode(RoleCode.ADMIN)
                .orElseGet(() -> roles.save(new Role(RoleCode.ADMIN, "Quản trị viên")));
        customer = saveUser("admin-order-status-customer@example.com", "Khách đặt hàng", "0906000001", customerRole);
        admin = saveUser("admin-order-status-admin@example.com", "Quản trị viên đơn hàng", "0906000002", adminRole);
        adminToken = tokenIssuer.issue(admin).accessToken();
        customerToken = tokenIssuer.issue(customer).accessToken();
    }

    @Test
    void adminCanProgressAnOrderAndEachChangeRecordsTheActor() throws Exception {
        Order order = saveOrder("TS-ADMIN-STATUS-PROGRESS");

        updateStatus(order.getId(), "confirmed")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.statusHistory", hasSize(2)))
                .andExpect(jsonPath("$.data.statusHistory[1].status").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.statusHistory[1].changedAt").isNotEmpty())
                .andExpect(jsonPath("$.data.statusHistory[1].changedBy.id").value(admin.getId()))
                .andExpect(jsonPath("$.data.statusHistory[1].changedBy.fullName").value("Quản trị viên đơn hàng"));

        updateStatus(order.getId(), "SHIPPING")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SHIPPING"))
                .andExpect(jsonPath("$.data.statusHistory", hasSize(3)))
                .andExpect(jsonPath("$.data.statusHistory[2].status").value("SHIPPING"))
                .andExpect(jsonPath("$.data.statusHistory[2].changedBy.id").value(admin.getId()));

        updateStatus(order.getId(), "COMPLETED")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.statusHistory", hasSize(4)))
                .andExpect(jsonPath("$.data.statusHistory[3].status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.statusHistory[3].changedBy.id").value(admin.getId()));

        updateStatus(order.getId(), "PENDING")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void adminCanCancelOnlyFromAnEligibleStatusAndCannotSkipWorkflowSteps() throws Exception {
        Order pendingOrder = saveOrder("TS-ADMIN-STATUS-CANCEL");
        updateStatus(pendingOrder.getId(), "CANCELLED")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"))
                .andExpect(jsonPath("$.data.statusHistory", hasSize(2)))
                .andExpect(jsonPath("$.data.statusHistory[1].status").value("CANCELLED"))
                .andExpect(jsonPath("$.data.statusHistory[1].changedBy.id").value(admin.getId()));

        Order anotherPendingOrder = saveOrder("TS-ADMIN-STATUS-SKIP");
        updateStatus(anotherPendingOrder.getId(), "SHIPPING")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void validatesTheRequestAndRestrictsStatusUpdatesToAdmins() throws Exception {
        Order order = saveOrder("TS-ADMIN-STATUS-VALIDATE");

        mockMvc.perform(patch("/api/v1/admin/orders/{id}/status", order.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        updateStatus(order.getId(), "UNKNOWN")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        mockMvc.perform(patch("/api/v1/admin/orders/-1/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CONFIRMED\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        updateStatus(999999L, "CONFIRMED")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));
        mockMvc.perform(patch("/api/v1/admin/orders/{id}/status", order.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CONFIRMED\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        mockMvc.perform(patch("/api/v1/admin/orders/{id}/status", order.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CONFIRMED\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminCancelOrderPersistsCancellationReasonAndActor() throws Exception {
        Order orderWithReason = saveOrder("TS-ADMIN-CANCEL-REASON");
        mockMvc.perform(patch("/api/v1/admin/orders/{id}/status", orderWithReason.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CANCELLED\",\"reason\":\"Sản phẩm hết hàng\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"))
                .andExpect(jsonPath("$.data.cancellationReason").value("Sản phẩm hết hàng"))
                .andExpect(jsonPath("$.data.statusHistory[1].status").value("CANCELLED"))
                .andExpect(jsonPath("$.data.statusHistory[1].changedBy.id").value(admin.getId()))
                .andExpect(jsonPath("$.data.statusHistory[1].changedBy.fullName").value("Quản trị viên đơn hàng"));

        // Cancel without reason uses default
        Order orderNoReason = saveOrder("TS-ADMIN-CANCEL-DEFAULT");
        mockMvc.perform(patch("/api/v1/admin/orders/{id}/status", orderNoReason.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CANCELLED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cancellationReason").value("Quản trị viên huỷ đơn hàng"))
                .andExpect(jsonPath("$.data.statusHistory[1].changedBy.id").value(admin.getId()));

        // Reason too long (> 500 chars) rejected
        String longReason = "x".repeat(501);
        mockMvc.perform(patch("/api/v1/admin/orders/{id}/status", orderNoReason.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CANCELLED\",\"reason\":\"" + longReason + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    private org.springframework.test.web.servlet.ResultActions updateStatus(Long orderId, String orderStatus) throws Exception {
        return mockMvc.perform(patch("/api/v1/admin/orders/{id}/status", orderId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"" + orderStatus + "\"}"));
    }

    private Order saveOrder(String orderNumber) {
        return orders.saveAndFlush(new Order(orderNumber, customer, PaymentMethod.COD,
                new BigDecimal("100000"), BigDecimal.ZERO, new BigDecimal("30000")));
    }

    private User saveUser(String email, String fullName, String phone, Role role) {
        User user = new User(email, "hash", fullName, phone);
        user.addRole(role);
        return users.saveAndFlush(user);
    }
}

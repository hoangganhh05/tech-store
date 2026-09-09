package com.techstore.e2e;

import com.techstore.entity.*;
import com.techstore.enums.*;
import com.techstore.repository.*;
import com.techstore.security.TokenIssuer;
import com.techstore.service.NotificationService;
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

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminNotificationIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired NotificationRepository notifications;
    @Autowired OrderRepository orders;
    @Autowired RoleRepository roles;
    @Autowired UserRepository users;
    @Autowired TokenIssuer tokenIssuer;
    @Autowired NotificationService notificationService;

    private User admin;
    private User customer;
    private String adminToken;
    private String customerToken;

    @BeforeEach
    void setUp() {
        Role adminRole = roles.findByCode(RoleCode.ADMIN)
                .orElseGet(() -> roles.save(new Role(RoleCode.ADMIN, "Quản trị viên")));
        Role customerRole = roles.findByCode(RoleCode.CUSTOMER)
                .orElseGet(() -> roles.save(new Role(RoleCode.CUSTOMER, "Khách hàng")));

        admin = saveUser("admin-notif@example.com", "Admin Notif", "0908000001", adminRole);
        customer = saveUser("cust-notif@example.com", "Customer Notif", "0908000002", customerRole);

        adminToken = tokenIssuer.issue(admin).accessToken();
        customerToken = tokenIssuer.issue(customer).accessToken();
    }

    @Test
    void adminReceivesNotificationWhenNewOrderIsCreated() throws Exception {
        Order order = saveOrder("TS-NOTIF-001");
        notificationService.createOrderNotification(order);

        mockMvc.perform(get("/api/v1/admin/notifications")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").value(1))
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.items[0].title").value("Đơn hàng mới #TS-NOTIF-001"))
                .andExpect(jsonPath("$.data.items[0].orderId").value(order.getId()))
                .andExpect(jsonPath("$.data.items[0].orderNumber").value("TS-NOTIF-001"))
                .andExpect(jsonPath("$.data.items[0].isRead").value(false));
    }

    @Test
    void adminCanMarkSingleNotificationAsRead() throws Exception {
        Order order = saveOrder("TS-NOTIF-002");
        notificationService.createOrderNotification(order);

        Notification notification = notifications.findByUserIdOrderByCreatedAtDesc(admin.getId(), null)
                .getContent().get(0);

        mockMvc.perform(patch("/api/v1/admin/notifications/{id}/read", notification.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isRead").value(true));

        mockMvc.perform(get("/api/v1/admin/notifications")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").value(0))
                .andExpect(jsonPath("$.data.items[0].isRead").value(true));
    }

    @Test
    void adminCanMarkAllNotificationsAsRead() throws Exception {
        Order order1 = saveOrder("TS-NOTIF-003A");
        Order order2 = saveOrder("TS-NOTIF-003B");
        notificationService.createOrderNotification(order1);
        notificationService.createOrderNotification(order2);

        mockMvc.perform(get("/api/v1/admin/notifications")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").value(2))
                .andExpect(jsonPath("$.data.items", hasSize(2)));

        mockMvc.perform(patch("/api/v1/admin/notifications/read-all")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(2));

        mockMvc.perform(get("/api/v1/admin/notifications")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").value(0))
                .andExpect(jsonPath("$.data.items[0].isRead").value(true))
                .andExpect(jsonPath("$.data.items[1].isRead").value(true));
    }

    @Test
    void customerCannotAccessAdminNotifications() throws Exception {
        mockMvc.perform(get("/api/v1/admin/notifications")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        mockMvc.perform(get("/api/v1/admin/notifications"))
                .andExpect(status().isUnauthorized());
    }

    private Order saveOrder(String orderNumber) {
        Order order = new Order(orderNumber, customer, PaymentMethod.COD,
                new BigDecimal("500000"), BigDecimal.ZERO, new BigDecimal("30000"));
        return orders.saveAndFlush(order);
    }

    private User saveUser(String email, String fullName, String phone, Role role) {
        User user = new User(email, "hash", fullName, phone);
        user.addRole(role);
        return users.saveAndFlush(user);
    }
}

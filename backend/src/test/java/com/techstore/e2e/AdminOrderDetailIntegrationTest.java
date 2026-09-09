package com.techstore.e2e;

import com.techstore.entity.Order;
import com.techstore.entity.OrderAddress;
import com.techstore.entity.OrderItem;
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
class AdminOrderDetailIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired OrderRepository orders;
    @Autowired RoleRepository roles;
    @Autowired UserRepository users;
    @Autowired TokenIssuer tokenIssuer;
    @Autowired EntityManager entityManager;

    private User customer;
    private String adminToken;
    private String customerToken;

    @BeforeEach
    void setUp() {
        Role customerRole = roles.findByCode(RoleCode.CUSTOMER)
                .orElseGet(() -> roles.save(new Role(RoleCode.CUSTOMER, "Khách hàng")));
        Role adminRole = roles.findByCode(RoleCode.ADMIN)
                .orElseGet(() -> roles.save(new Role(RoleCode.ADMIN, "Quản trị viên")));
        customer = saveUser("admin-order-detail-customer@example.com", "Nguyễn Văn An", "0901000001", customerRole);
        User admin = saveUser("admin-order-detail-admin@example.com", "Admin", "0902000002", adminRole);
        adminToken = tokenIssuer.issue(admin).accessToken();
        customerToken = tokenIssuer.issue(customer).accessToken();
    }

    @Test
    void adminCanViewAllOrderInformationIncludingInternalNote() throws Exception {
        Order order = saveCompleteOrder();
        entityManager.clear();

        mockMvc.perform(get("/api/v1/admin/orders/{id}", order.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(order.getId()))
                .andExpect(jsonPath("$.data.orderNumber").value("TS-ADMIN-DETAIL"))
                .andExpect(jsonPath("$.data.customer.fullName").value("Nguyễn Văn An"))
                .andExpect(jsonPath("$.data.customer.email").value("admin-order-detail-customer@example.com"))
                .andExpect(jsonPath("$.data.customer.phone").value("0901000001"))
                .andExpect(jsonPath("$.data.status").value("SHIPPING"))
                .andExpect(jsonPath("$.data.paymentMethod").value("BANK_TRANSFER"))
                .andExpect(jsonPath("$.data.paymentStatus").value("PAID"))
                .andExpect(jsonPath("$.data.internalNote").value("Gọi khách trước khi giao hàng"))
                .andExpect(jsonPath("$.data.shippingAddress.recipientName").value("Nguyễn Văn An"))
                .andExpect(jsonPath("$.data.shippingAddress.district").value("Cầu Giấy"))
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.items[0].productName").value("Điện thoại TechStore"))
                .andExpect(jsonPath("$.data.items[0].sku").value("PHONE-BLACK-128"))
                .andExpect(jsonPath("$.data.items[0].quantity").value(2))
                .andExpect(jsonPath("$.data.items[0].subtotal").value(1000000))
                .andExpect(jsonPath("$.data.statusHistory", hasSize(3)))
                .andExpect(jsonPath("$.data.statusHistory[2].status").value("SHIPPING"))
                .andExpect(jsonPath("$.data.subtotal").value(1000000))
                .andExpect(jsonPath("$.data.discountAmount").value(100000))
                .andExpect(jsonPath("$.data.shippingFee").value(30000))
                .andExpect(jsonPath("$.data.totalAmount").value(930000));

        mockMvc.perform(get("/api/v1/orders/{id}", order.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.internalNote").doesNotExist());
    }

    @Test
    void validatesIdHandlesMissingOrdersAndRestrictsDetailsToAdmins() throws Exception {
        mockMvc.perform(get("/api/v1/admin/orders/-1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        mockMvc.perform(get("/api/v1/admin/orders/999999")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));
        mockMvc.perform(get("/api/v1/admin/orders/1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        mockMvc.perform(get("/api/v1/admin/orders/1"))
                .andExpect(status().isUnauthorized());
    }

    private Order saveCompleteOrder() {
        Order order = new Order("TS-ADMIN-DETAIL", customer, PaymentMethod.BANK_TRANSFER,
                new BigDecimal("1000000"), new BigDecimal("100000"), new BigDecimal("30000"));
        order.setInternalNote("Gọi khách trước khi giao hàng");
        order.setShippingAddress(new OrderAddress(order, "Nguyễn Văn An", "0901000001", "1 Duy Tân",
                "Dịch Vọng", "Cầu Giấy", "Hà Nội"));
        order.addItem(new OrderItem(101L, "Điện thoại TechStore", "PHONE-BLACK-128", "Đen / 128GB",
                new BigDecimal("500000"), 2));
        orders.saveAndFlush(order);
        entityManager.createNativeQuery("UPDATE orders SET status = :status, payment_status = :paymentStatus WHERE id = :id")
                .setParameter("status", "SHIPPING")
                .setParameter("paymentStatus", "PAID")
                .setParameter("id", order.getId())
                .executeUpdate();
        entityManager.createNativeQuery("INSERT INTO order_status_history (order_id, status, changed_at) VALUES (:orderId, :status, :changedAt)")
                .setParameter("orderId", order.getId())
                .setParameter("status", "CONFIRMED")
                .setParameter("changedAt", Timestamp.from(Instant.parse("2026-09-09T10:30:00Z")))
                .executeUpdate();
        entityManager.createNativeQuery("INSERT INTO order_status_history (order_id, status, changed_at) VALUES (:orderId, :status, :changedAt)")
                .setParameter("orderId", order.getId())
                .setParameter("status", "SHIPPING")
                .setParameter("changedAt", Timestamp.from(Instant.parse("2026-09-09T11:00:00Z")))
                .executeUpdate();
        return order;
    }

    private User saveUser(String email, String fullName, String phone, Role role) {
        User user = new User(email, "hash", fullName, phone);
        user.addRole(role);
        return users.saveAndFlush(user);
    }
}

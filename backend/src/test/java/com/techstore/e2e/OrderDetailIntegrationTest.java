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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class OrderDetailIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired OrderRepository orders;
    @Autowired RoleRepository roles;
    @Autowired UserRepository users;
    @Autowired TokenIssuer tokenIssuer;

    private User customer;
    private User otherCustomer;
    private String customerToken;
    private String otherCustomerToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        Role customerRole = roles.findByCode(RoleCode.CUSTOMER)
                .orElseGet(() -> roles.save(new Role(RoleCode.CUSTOMER, "Khách hàng")));
        Role adminRole = roles.findByCode(RoleCode.ADMIN)
                .orElseGet(() -> roles.save(new Role(RoleCode.ADMIN, "Quản trị viên")));

        customer = saveUser("order-detail-customer@example.com", customerRole);
        otherCustomer = saveUser("order-detail-other@example.com", customerRole);
        User admin = saveUser("order-detail-admin@example.com", adminRole);
        customerToken = tokenIssuer.issue(customer).accessToken();
        otherCustomerToken = tokenIssuer.issue(otherCustomer).accessToken();
        adminToken = tokenIssuer.issue(admin).accessToken();
    }

    @Test
    void ownerCanViewProductsAddressPaymentMethodAndStatusTimeline() throws Exception {
        Order order = saveOrder(customer, "TS-DETAIL-OWNER");

        mockMvc.perform(get("/api/v1/orders/{id}", order.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderNumber").value("TS-DETAIL-OWNER"))
                .andExpect(jsonPath("$.data.paymentMethod").value("COD"))
                .andExpect(jsonPath("$.data.shippingAddress.recipientName").value("Nguyễn Văn A"))
                .andExpect(jsonPath("$.data.shippingAddress.province").value("Hà Nội"))
                .andExpect(jsonPath("$.data.items[0].productName").value("Điện thoại"))
                .andExpect(jsonPath("$.data.items[0].variantLabel").value("Đen / 128GB"))
                .andExpect(jsonPath("$.data.items[0].quantity").value(2))
                .andExpect(jsonPath("$.data.items[0].unitPrice").value(500000))
                .andExpect(jsonPath("$.data.statusHistory[0].status").value("PENDING"));
    }

    @Test
    void adminCanViewButAnotherCustomerCannot() throws Exception {
        Order order = saveOrder(customer, "TS-DETAIL-ACCESS");

        mockMvc.perform(get("/api/v1/orders/{id}", order.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/orders/{id}", order.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherCustomerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void validatesAuthenticationAndOrderId() throws Exception {
        mockMvc.perform(get("/api/v1/orders/999999")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));
        mockMvc.perform(get("/api/v1/orders/not-a-number")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        mockMvc.perform(get("/api/v1/orders/1"))
                .andExpect(status().isUnauthorized());
    }

    private User saveUser(String email, Role role) {
        User user = new User(email, "hash", "Test user", "0900000000");
        user.addRole(role);
        return users.saveAndFlush(user);
    }

    private Order saveOrder(User user, String number) {
        Order order = new Order(number, user, PaymentMethod.COD,
                new BigDecimal("1000000"), BigDecimal.ZERO, new BigDecimal("30000"));
        order.setShippingAddress(new OrderAddress(order, "Nguyễn Văn A", "0900000000", "1 Duy Tân", "Dịch Vọng",
                "Cầu Giấy", "Hà Nội"));
        order.addItem(new OrderItem(1L, "Điện thoại", "PHONE-BLACK-128", "Đen / 128GB",
                new BigDecimal("500000"), 2));
        return orders.saveAndFlush(order);
    }
}

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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CheckoutPaymentIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired OrderRepository orders;
    @Autowired TokenIssuer tokens;
    @Autowired EntityManager entityManager;
    private User customer;
    private String token;

    @BeforeEach
    void setup() {
        Role role = roles.findByCode(RoleCode.CUSTOMER)
                .orElseGet(() -> roles.save(new Role(RoleCode.CUSTOMER, "Customer")));
        customer = new User("payment.test@example.com", "hash", "Payment customer", "0912345678");
        customer.addRole(role);
        customer = users.saveAndFlush(customer);
        token = "Bearer " + tokens.issue(customer).accessToken();
    }

    @Test
    void listsSupportedMethods() throws Exception {
        mvc.perform(get("/api/v1/checkout/payment-methods").header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data", hasSize(3)))
                .andExpect(jsonPath("$.data[0].paymentMethod").value("COD"))
                .andExpect(jsonPath("$.data[1].instructions").isNotEmpty())
                .andExpect(jsonPath("$.data[2].paymentMethod").value("ONLINE"));
    }

    @ParameterizedTest
    @EnumSource(PaymentMethod.class)
    void acceptsEachMethodAndPersistsItWhenAnOrderIsCreated(PaymentMethod method) throws Exception {
        long count = orders.count();
        mvc.perform(post("/api/v1/checkout/payment-method").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"paymentMethod\":\"" + method + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.paymentMethod").value(method.name()));
        assertThat(orders.count()).isEqualTo(count); // Selection must not place an order.
        Order order = orders.saveAndFlush(new Order("TEST-" + method, customer, method,
                new BigDecimal("100000"), BigDecimal.ZERO, new BigDecimal("30000")));
        Long id = order.getId();
        entityManager.clear();
        Order stored = orders.findById(id).orElseThrow();
        assertThat(stored.getPaymentMethod()).isEqualTo(method);
        assertThat(stored.getPaymentStatus()).isEqualTo("UNPAID");
        assertThat(entityManager.createNativeQuery("select payment_method from orders where id = :id")
                .setParameter("id", id).getSingleResult()).isEqualTo(method.name());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"paymentMethod\":null}", "{\"paymentMethod\":\"INVALID\"}",
            "{\"paymentMethod\":\"\"}", "{\"paymentMethod\":0}", "{\"paymentMethod\":true}", "{", ""})
    void rejectsInvalidSelection(String body) throws Exception {
        mvc.perform(post("/api/v1/checkout/payment-method").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void requiresCustomerSessionForBothEndpoints() throws Exception {
        mvc.perform(get("/api/v1/checkout/payment-methods")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/checkout/payment-method").contentType(MediaType.APPLICATION_JSON)
                .content("{\"paymentMethod\":\"COD\"}")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/checkout/payment-methods").header("Authorization", "Bearer invalid"))
                .andExpect(status().isUnauthorized());
        Role adminRole = roles.findByCode(RoleCode.ADMIN)
                .orElseGet(() -> roles.save(new Role(RoleCode.ADMIN, "Admin")));
        User admin = new User("payment.admin@example.com", "hash", "Admin", "0912345678");
        admin.addRole(adminRole);
        String adminToken = "Bearer " + tokens.issue(users.saveAndFlush(admin)).accessToken();
        mvc.perform(get("/api/v1/checkout/payment-methods").header("Authorization", adminToken))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/checkout/payment-method").header("Authorization", adminToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"paymentMethod\":\"COD\"}"))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"addressId\":0,\"paymentMethod\":\"COD\"}",
            "{\"addressId\":1}", "{\"addressId\":1,\"paymentMethod\":\"INVALID\"}", "{"})
    void reviewRejectsInvalidInput(String body) throws Exception {
        mvc.perform(post("/api/v1/checkout/review").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void reviewRequiresCustomerAndDoesNotExposeAnotherUsersAddress() throws Exception {
        mvc.perform(post("/api/v1/checkout/review").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"addressId\":1,\"paymentMethod\":\"COD\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/checkout/review").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"addressId\":999999,\"paymentMethod\":\"COD\"}"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("ADDRESS_NOT_FOUND"));
    }

    @Test
    void placeOrderRequiresCustomerAuthentication() throws Exception {
        mvc.perform(post("/api/v1/orders").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"addressId\":1,\"paymentMethod\":\"COD\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/orders").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}

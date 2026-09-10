package com.techstore.e2e;

import com.techstore.entity.Order;
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
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminRevenueReportIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired OrderRepository orders;
    @Autowired RoleRepository roles;
    @Autowired UserRepository users;
    @Autowired TokenIssuer tokenIssuer;

    private String adminToken;
    private String customerToken;

    @BeforeEach
    void setUp() {
        Role adminRole = roles.findByCode(RoleCode.ADMIN)
                .orElseGet(() -> roles.save(new Role(RoleCode.ADMIN, "Quản trị viên")));
        Role customerRole = roles.findByCode(RoleCode.CUSTOMER)
                .orElseGet(() -> roles.save(new Role(RoleCode.CUSTOMER, "Khách hàng")));
        User admin = saveUser("revenue-admin@example.com", "Revenue Admin", "0907000011", adminRole);
        User customer = saveUser("revenue-customer@example.com", "Revenue Customer", "0907000012", customerRole);
        adminToken = tokenIssuer.issue(admin).accessToken();
        customerToken = tokenIssuer.issue(customer).accessToken();
    }

    @Test
    void revenueReportAggregatesTotalsDailyDetailsAndExcludesCancelledOrders() throws Exception {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        saveOrder("TS-REVENUE-ONE", "Điện thoại Alpha", 2,
                new BigDecimal("100000"), new BigDecimal("20000"), "COMPLETED");
        saveOrder("TS-REVENUE-TWO", "Tai nghe Beta", 1,
                new BigDecimal("150000"), BigDecimal.ZERO, "PENDING");
        saveOrder("TS-REVENUE-CANCELLED", "Sản phẩm bị huỷ", 1,
                new BigDecimal("900000"), BigDecimal.ZERO, "CANCELLED");

        mockMvc.perform(get("/api/v1/admin/reports/revenue")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .param("fromDate", today.toString())
                        .param("toDate", today.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fromDate").value(today.toString()))
                .andExpect(jsonPath("$.data.toDate").value(today.toString()))
                .andExpect(jsonPath("$.data.totalOrders").value(2))
                .andExpect(jsonPath("$.data.totalRevenue").value(370000))
                .andExpect(jsonPath("$.data.averageOrderValue").value(185000))
                .andExpect(jsonPath("$.data.dailyRevenue", hasSize(1)))
                .andExpect(jsonPath("$.data.dailyRevenue[0].date").value(today.toString()))
                .andExpect(jsonPath("$.data.dailyRevenue[0].revenue").value(370000))
                .andExpect(jsonPath("$.data.dailyRevenue[0].orderCount").value(2))
                .andExpect(jsonPath("$.data.dailyRevenue[0].averageOrderValue").value(185000));
    }

    @Test
    void revenueReportReturnsZeroRowsForDaysWithoutOrders() throws Exception {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate tomorrow = today.plusDays(1);
        mockMvc.perform(get("/api/v1/admin/reports/revenue")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .param("fromDate", today.toString())
                        .param("toDate", tomorrow.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.dailyRevenue", hasSize(2)))
                .andExpect(jsonPath("$.data.dailyRevenue[0].orderCount").value(0))
                .andExpect(jsonPath("$.data.dailyRevenue[1].revenue").value(0));
    }

    @Test
    void revenueReportRequiresAdminAndValidatesDateRange() throws Exception {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        mockMvc.perform(get("/api/v1/admin/reports/revenue")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken)
                        .param("fromDate", today.toString())
                        .param("toDate", today.toString()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        mockMvc.perform(get("/api/v1/admin/reports/revenue")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .param("fromDate", today.plusDays(1).toString())
                        .param("toDate", today.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    private Order saveOrder(String orderNumber, String productName, int quantity,
                            BigDecimal unitPrice, BigDecimal shippingFee, String status) {
        User customer = users.findByEmailIgnoreCase("revenue-customer@example.com").orElseThrow();
        BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
        Order order = new Order(orderNumber, customer, PaymentMethod.COD, subtotal, BigDecimal.ZERO, shippingFee);
        order.addItem(new OrderItem(100L, productName, orderNumber + "-SKU", null, unitPrice, quantity));
        if ("CANCELLED".equals(status)) {
            order.cancel("Đơn kiểm thử");
        } else if (!"PENDING".equals(status)) {
            order.updateStatus(status, null);
        }
        return orders.saveAndFlush(order);
    }

    private User saveUser(String email, String fullName, String phone, Role role) {
        User user = new User(email, "hash", fullName, phone);
        user.addRole(role);
        return users.saveAndFlush(user);
    }
}

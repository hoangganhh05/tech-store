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
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminDashboardIntegrationTest {

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
        User admin = saveUser("dashboard-admin@example.com", "Dashboard Admin", "0907000001", adminRole);
        User customer = saveUser("dashboard-customer@example.com", "Dashboard Customer", "0907000002", customerRole);
        adminToken = tokenIssuer.issue(admin).accessToken();
        customerToken = tokenIssuer.issue(customer).accessToken();
    }

    @Test
    void adminDashboardAggregatesValidOrdersByDayAndExcludesCancelledOrders() throws Exception {
        saveOrder("TS-DASH-PENDING", "Điện thoại Alpha", 2, new BigDecimal("100000"), new BigDecimal("50000"), "PENDING");
        saveOrder("TS-DASH-COMPLETED", "Tai nghe Beta", 1, new BigDecimal("150000"), BigDecimal.ZERO, "COMPLETED");
        saveOrder("TS-DASH-CANCELLED", "Sản phẩm bị huỷ", 9, new BigDecimal("900000"), BigDecimal.ZERO, "CANCELLED");

        mockMvc.perform(get("/api/v1/admin/dashboard")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .param("period", "DAY")
                        .param("date", LocalDate.now(ZoneOffset.UTC).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.period").value("DAY"))
                .andExpect(jsonPath("$.data.totalOrders").value(2))
                .andExpect(jsonPath("$.data.totalRevenue").value(400000))
                .andExpect(jsonPath("$.data.ordersByStatus", hasSize(4)))
                .andExpect(jsonPath("$.data.ordersByStatus[0].status").value("PENDING"))
                .andExpect(jsonPath("$.data.ordersByStatus[0].count").value(1))
                .andExpect(jsonPath("$.data.ordersByStatus[3].status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.ordersByStatus[3].count").value(1))
                .andExpect(jsonPath("$.data.topSellingProducts[0].productName").value("Điện thoại Alpha"))
                .andExpect(jsonPath("$.data.topSellingProducts[0].quantitySold").value(2))
                .andExpect(jsonPath("$.data.revenueTrend", hasSize(24)));
    }

    @Test
    void monthDashboardReturnsOneRevenuePointPerDay() throws Exception {
        LocalDate month = LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1);
        saveOrder("TS-DASH-MONTH", "Sản phẩm tháng", 1, new BigDecimal("200000"), BigDecimal.ZERO, "SHIPPING");

        mockMvc.perform(get("/api/v1/admin/dashboard")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .param("period", "MONTH")
                        .param("date", month.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fromDate").value(month.toString()))
                .andExpect(jsonPath("$.data.toDate").value(month.withDayOfMonth(YearMonth.from(month).lengthOfMonth()).toString()))
                .andExpect(jsonPath("$.data.revenueTrend", hasSize(YearMonth.from(month).lengthOfMonth())))
                .andExpect(jsonPath("$.data.ordersByStatus[2].status").value("SHIPPING"))
                .andExpect(jsonPath("$.data.ordersByStatus[2].count").value(1));
    }

    @Test
    void weekDashboardStartsOnMondayAndReturnsSevenDailyRevenuePoints() throws Exception {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        saveOrder("TS-DASH-WEEK", "Sản phẩm tuần", 1, new BigDecimal("200000"), BigDecimal.ZERO, "COMPLETED");

        mockMvc.perform(get("/api/v1/admin/dashboard")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .param("period", "WEEK")
                        .param("date", today.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.period").value("WEEK"))
                .andExpect(jsonPath("$.data.fromDate").value(weekStart.toString()))
                .andExpect(jsonPath("$.data.toDate").value(weekStart.plusDays(6).toString()))
                .andExpect(jsonPath("$.data.totalOrders").value(1))
                .andExpect(jsonPath("$.data.revenueTrend", hasSize(7)));
    }

    @Test
    void dashboardRequiresAdminAndValidatesPeriod() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        mockMvc.perform(get("/api/v1/admin/dashboard")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .param("period", "YEAR"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    private Order saveOrder(String orderNumber, String productName, int quantity,
                            BigDecimal unitPrice, BigDecimal shippingFee, String status) {
        User customer = users.findByEmailIgnoreCase("dashboard-customer@example.com").orElseThrow();
        BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
        Order order = new Order(orderNumber, customer, PaymentMethod.COD, subtotal, BigDecimal.ZERO, shippingFee);
        order.addItem(new OrderItem(100L, productName, orderNumber + "-SKU", null, unitPrice, quantity));
        if (!"PENDING".equals(status)) {
            if ("CANCELLED".equals(status)) {
                order.cancel("Đơn kiểm thử");
            } else {
                order.updateStatus(status, null);
            }
        }
        return orders.saveAndFlush(order);
    }

    private User saveUser(String email, String fullName, String phone, Role role) {
        User user = new User(email, "hash", fullName, phone);
        user.addRole(role);
        return users.saveAndFlush(user);
    }
}

package com.techstore.e2e;

import com.techstore.entity.*;
import com.techstore.enums.*;
import com.techstore.event.OrderStatusUpdatedEvent;
import com.techstore.infrastructure.mail.OrderStatusUpdateEmailSender;
import com.techstore.repository.*;
import com.techstore.security.TokenIssuer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@RecordApplicationEvents
class OrderStatusEmailIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired OrderRepository orders;
    @Autowired RoleRepository roles;
    @Autowired UserRepository users;
    @Autowired CategoryRepository categories;
    @Autowired BrandRepository brands;
    @Autowired ProductRepository products;
    @Autowired ProductVariantRepository variants;
    @Autowired InventoryRepository inventories;
    @Autowired TokenIssuer tokenIssuer;
    @Autowired ApplicationEvents applicationEvents;

    @MockitoBean OrderStatusUpdateEmailSender emailSender;

    private User customer;
    private User admin;
    private String adminToken;
    private String customerToken;
    private ProductVariant variant;

    @BeforeEach
    void setUp() {
        Role customerRole = roles.findByCode(RoleCode.CUSTOMER)
                .orElseGet(() -> roles.save(new Role(RoleCode.CUSTOMER, "Khách hàng")));
        Role adminRole = roles.findByCode(RoleCode.ADMIN)
                .orElseGet(() -> roles.save(new Role(RoleCode.ADMIN, "Quản trị viên")));
        customer = saveUser("order-status-email-customer@example.com", "Nguyễn Khách Hàng", "0906000001", customerRole);
        admin = saveUser("order-status-email-admin@example.com", "Quản trị viên đơn hàng", "0906000002", adminRole);
        adminToken = tokenIssuer.issue(admin).accessToken();
        customerToken = tokenIssuer.issue(customer).accessToken();

        Category category = categories.save(new Category("Tai nghe", "", null, null));
        Brand brand = brands.save(new Brand("TechAudio", null, ""));
        Product product = products.save(new Product("Tai nghe gaming", "", brand, category, ProductStatus.ACTIVE));
        variant = variants.save(new ProductVariant(product, "HEADSET-01", "Đen", "",
                new BigDecimal("350000"), null, 10, VariantStatus.ACTIVE));
        inventories.saveAndFlush(new Inventory(variant, 10, 0, 5));
    }

    @Test
    void adminUpdatingStatusPublishesOrderStatusUpdatedEventWithCorrectDetails() throws Exception {
        Order order = saveOrderWithItems("TS-EMAIL-STATUS-CONFIRM");

        mockMvc.perform(patch("/api/v1/admin/orders/{id}/status", order.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CONFIRMED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"));

        var events = applicationEvents.stream(OrderStatusUpdatedEvent.class)
                .filter(e -> order.getOrderNumber().equals(e.orderNumber()) && "CONFIRMED".equals(e.status()))
                .toList();

        assertThat(events).hasSize(1);
        OrderStatusUpdatedEvent event = events.get(0);
        assertThat(event.recipientEmail()).isEqualTo("order-status-email-customer@example.com");
        assertThat(event.recipientName()).isEqualTo("Nguyễn Khách Hàng");
        assertThat(event.status()).isEqualTo("CONFIRMED");
        assertThat(event.cancellationReason()).isNull();
        assertThat(event.deliveryAddress()).contains("Cầu Giấy", "Hà Nội");
        assertThat(event.recipientPhone()).isEqualTo("0906000001");
        assertThat(event.items()).hasSize(1);
        assertThat(event.items().get(0).productName()).isEqualTo("Tai nghe gaming");
    }

    @Test
    void adminCancellingOrderPublishesCancelledEventWithReason() throws Exception {
        Order order = saveOrderWithItems("TS-EMAIL-STATUS-ADMIN-CANCEL");

        mockMvc.perform(patch("/api/v1/admin/orders/{id}/status", order.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CANCELLED\",\"reason\":\"Hết hàng trong kho\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));

        var events = applicationEvents.stream(OrderStatusUpdatedEvent.class)
                .filter(e -> order.getOrderNumber().equals(e.orderNumber()) && "CANCELLED".equals(e.status()))
                .toList();

        assertThat(events).hasSize(1);
        OrderStatusUpdatedEvent event = events.get(0);
        assertThat(event.status()).isEqualTo("CANCELLED");
        assertThat(event.cancellationReason()).isEqualTo("Hết hàng trong kho");
    }

    @Test
    void customerCancellingOrderPublishesCancelledEvent() throws Exception {
        Order order = saveOrderWithItems("TS-EMAIL-STATUS-CUST-CANCEL");

        mockMvc.perform(patch("/api/v1/orders/{id}/cancel", order.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Tôi muốn đổi địa chỉ nhận\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));

        var events = applicationEvents.stream(OrderStatusUpdatedEvent.class)
                .filter(e -> order.getOrderNumber().equals(e.orderNumber()) && "CANCELLED".equals(e.status()))
                .toList();

        assertThat(events).hasSize(1);
        OrderStatusUpdatedEvent event = events.get(0);
        assertThat(event.status()).isEqualTo("CANCELLED");
        assertThat(event.cancellationReason()).isEqualTo("Tôi muốn đổi địa chỉ nhận");
    }

    @Test
    void mailSenderFailureDoesNotRollbackOrFailOrderStatusUpdate() throws Exception {
        doThrow(new RuntimeException("Simulated SMTP network outage")).when(emailSender).send(any());

        Order order = saveOrderWithItems("TS-EMAIL-STATUS-FAILSAFE");

        mockMvc.perform(patch("/api/v1/admin/orders/{id}/status", order.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CONFIRMED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"));

        // Verify order status in database is persisted despite mail error
        Order updated = orders.findById(order.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo("CONFIRMED");
    }

    private Order saveOrderWithItems(String orderNumber) {
        Order order = new Order(orderNumber, customer, PaymentMethod.COD,
                new BigDecimal("350000"), BigDecimal.ZERO, new BigDecimal("30000"));
        order.setShippingAddress(new OrderAddress(order, "Nguyễn Khách Hàng", "0906000001", "100 Cầu Giấy",
                "Quan Hoa", "Cầu Giấy", "Hà Nội"));
        order.addItem(new OrderItem(variant.getId(), "Tai nghe gaming", variant.getSku(), "Đen",
                new BigDecimal("350000"), 1));
        return orders.saveAndFlush(order);
    }

    private User saveUser(String email, String fullName, String phone, Role role) {
        User user = new User(email, "hash", fullName, phone);
        user.addRole(role);
        return users.saveAndFlush(user);
    }
}

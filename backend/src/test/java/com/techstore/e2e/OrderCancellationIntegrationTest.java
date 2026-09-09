package com.techstore.e2e;

import com.techstore.entity.Brand;
import com.techstore.entity.Category;
import com.techstore.entity.Inventory;
import com.techstore.entity.Order;
import com.techstore.entity.OrderItem;
import com.techstore.entity.Product;
import com.techstore.entity.ProductVariant;
import com.techstore.entity.Role;
import com.techstore.entity.User;
import com.techstore.enums.InventoryTransactionType;
import com.techstore.enums.PaymentMethod;
import com.techstore.enums.ProductStatus;
import com.techstore.enums.RoleCode;
import com.techstore.enums.VariantStatus;
import com.techstore.repository.BrandRepository;
import com.techstore.repository.CategoryRepository;
import com.techstore.repository.InventoryRepository;
import com.techstore.repository.InventoryTransactionRepository;
import com.techstore.repository.OrderRepository;
import com.techstore.repository.ProductRepository;
import com.techstore.repository.ProductVariantRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class OrderCancellationIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired OrderRepository orders;
    @Autowired RoleRepository roles;
    @Autowired UserRepository users;
    @Autowired CategoryRepository categories;
    @Autowired BrandRepository brands;
    @Autowired ProductRepository products;
    @Autowired ProductVariantRepository variants;
    @Autowired InventoryRepository inventories;
    @Autowired InventoryTransactionRepository transactions;
    @Autowired TokenIssuer tokenIssuer;
    @Autowired EntityManager entityManager;

    private User customer;
    private User otherCustomer;
    private String customerToken;
    private String otherCustomerToken;
    private ProductVariant variant;

    @BeforeEach
    void setUp() {
        Role customerRole = roles.findByCode(RoleCode.CUSTOMER)
                .orElseGet(() -> roles.save(new Role(RoleCode.CUSTOMER, "Khách hàng")));
        customer = saveUser("order-cancel-customer@example.com", customerRole);
        otherCustomer = saveUser("order-cancel-other@example.com", customerRole);
        customerToken = tokenIssuer.issue(customer).accessToken();
        otherCustomerToken = tokenIssuer.issue(otherCustomer).accessToken();

        Category category = categories.save(new Category("Điện thoại huỷ đơn", "", null, null));
        Brand brand = brands.save(new Brand("Thương hiệu huỷ đơn", null, ""));
        Product product = products.save(new Product("Sản phẩm huỷ đơn", "", brand, category, ProductStatus.ACTIVE));
        variant = variants.save(new ProductVariant(product, "CANCEL-TEST-SKU", "Đen", "128GB",
                new BigDecimal("500000"), null, 3, VariantStatus.ACTIVE));
        inventories.saveAndFlush(new Inventory(variant, 3, 0, 5));
    }

    @Test
    void ownerCanCancelPendingOrderAndRestoreInventoryWithReason() throws Exception {
        Order order = saveOrder(customer, "TS-CANCEL-OWNER", "PENDING", 2);

        mockMvc.perform(patch("/api/v1/orders/{id}/cancel", order.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken)
                        .contentType("application/json")
                        .content("{\"reason\":\"Đổi ý không muốn mua nữa\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"))
                .andExpect(jsonPath("$.data.cancellationReason").value("Đổi ý không muốn mua nữa"));

        entityManager.clear();
        Order cancelled = orders.findById(order.getId()).orElseThrow();
        Inventory inventory = inventories.findByVariantId(variant.getId()).orElseThrow();
        assertThat(cancelled.getStatus()).isEqualTo("CANCELLED");
        assertThat(cancelled.getCancellationReason()).isEqualTo("Đổi ý không muốn mua nữa");
        assertThat(inventory.getQuantityOnHand()).isEqualTo(5);
        assertThat(transactions.findByInventoryIdOrderByCreatedAtDesc(inventory.getId()))
                .extracting(transaction -> transaction.getTransactionType())
                .contains(InventoryTransactionType.CANCEL_RETURN);
    }

    @Test
    void usesDefaultReasonAndRejectsOtherCustomerOrIneligibleStatus() throws Exception {
        Order pending = saveOrder(customer, "TS-CANCEL-ACCESS", "PENDING", 1);
        mockMvc.perform(patch("/api/v1/orders/{id}/cancel", pending.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherCustomerToken)
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        Order shipping = saveOrder(customer, "TS-CANCEL-SHIPPING", "SHIPPING", 1);
        mockMvc.perform(patch("/api/v1/orders/{id}/cancel", shipping.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken)
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ORDER_CANNOT_CANCEL"));

        mockMvc.perform(patch("/api/v1/orders/999999/cancel")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken)
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));
    }

    @Test
    void requiresCustomerAuthentication() throws Exception {
        mockMvc.perform(patch("/api/v1/orders/1/cancel")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    private User saveUser(String email, Role role) {
        User user = new User(email, "hash", "Customer", "0900000000");
        user.addRole(role);
        return users.saveAndFlush(user);
    }

    private Order saveOrder(User user, String number, String status, int quantity) {
        Order order = new Order(number, user, PaymentMethod.COD,
                new BigDecimal("1000000"), BigDecimal.ZERO, new BigDecimal("30000"));
        order.addItem(new OrderItem(variant.getId(), "Sản phẩm huỷ đơn", variant.getSku(), "Đen / 128GB",
                new BigDecimal("500000"), quantity));
        order = orders.saveAndFlush(order);
        if (!"PENDING".equals(status)) {
            entityManager.createNativeQuery("UPDATE orders SET status = :status WHERE id = :id")
                    .setParameter("status", status).setParameter("id", order.getId()).executeUpdate();
            entityManager.clear();
        }
        return order;
    }
}

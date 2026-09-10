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
import com.techstore.enums.PaymentMethod;
import com.techstore.enums.ProductStatus;
import com.techstore.enums.RoleCode;
import com.techstore.enums.VariantStatus;
import com.techstore.repository.BrandRepository;
import com.techstore.repository.CategoryRepository;
import com.techstore.repository.InventoryRepository;
import com.techstore.repository.OrderRepository;
import com.techstore.repository.ProductRepository;
import com.techstore.repository.ProductVariantRepository;
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
class AdminProductInventoryReportIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired BrandRepository brands;
    @Autowired CategoryRepository categories;
    @Autowired InventoryRepository inventories;
    @Autowired OrderRepository orders;
    @Autowired ProductRepository products;
    @Autowired ProductVariantRepository variants;
    @Autowired RoleRepository roles;
    @Autowired UserRepository users;
    @Autowired TokenIssuer tokenIssuer;

    private String adminToken;
    private String customerToken;
    private User customer;

    @BeforeEach
    void setUp() {
        Role adminRole = roles.findByCode(RoleCode.ADMIN)
                .orElseGet(() -> roles.save(new Role(RoleCode.ADMIN, "Quản trị viên")));
        Role customerRole = roles.findByCode(RoleCode.CUSTOMER)
                .orElseGet(() -> roles.save(new Role(RoleCode.CUSTOMER, "Khách hàng")));
        User admin = saveUser("product-report-admin@example.com", "Product Report Admin", "0907000021", adminRole);
        customer = saveUser("product-report-customer@example.com", "Product Report Customer", "0907000022", customerRole);
        adminToken = tokenIssuer.issue(admin).accessToken();
        customerToken = tokenIssuer.issue(customer).accessToken();
    }

    @Test
    void productReportReturnsTopSellingAndLowStockForSelectedCategory() throws Exception {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        Category phones = categories.saveAndFlush(new Category("Điện thoại báo cáo", null, null, null));
        Category audio = categories.saveAndFlush(new Category("Âm thanh báo cáo", null, null, null));
        Brand brand = brands.saveAndFlush(new Brand("Brand báo cáo", null, null));
        Product phone = products.saveAndFlush(new Product("Điện thoại Alpha báo cáo", null, brand, phones, ProductStatus.ACTIVE));
        Product headset = products.saveAndFlush(new Product("Tai nghe Beta báo cáo", null, brand, audio, ProductStatus.ACTIVE));
        ProductVariant phoneVariant = variants.saveAndFlush(new ProductVariant(phone, "RPT-PHONE", null, "128GB",
                new BigDecimal("100000"), null, 4, VariantStatus.ACTIVE));
        ProductVariant headsetVariant = variants.saveAndFlush(new ProductVariant(headset, "RPT-HEADSET", null, null,
                new BigDecimal("50000"), null, 20, VariantStatus.ACTIVE));
        inventories.saveAndFlush(new Inventory(phoneVariant, 4, 1, 5));
        inventories.saveAndFlush(new Inventory(headsetVariant, 20, 0, 5));
        saveOrder("TS-PRODUCT-REPORT-PHONE", phoneVariant.getId(), phone.getName(), 3,
                new BigDecimal("100000"), "COMPLETED");
        saveOrder("TS-PRODUCT-REPORT-HEADSET", headsetVariant.getId(), headset.getName(), 5,
                new BigDecimal("50000"), "COMPLETED");

        mockMvc.perform(get("/api/v1/admin/reports/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .param("fromDate", today.toString())
                        .param("toDate", today.toString())
                        .param("categoryId", phones.getId().toString())
                        .param("sortBy", "QUANTITY")
                        .param("sortDirection", "DESC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.categoryId").value(phones.getId()))
                .andExpect(jsonPath("$.data.topSellingProducts", hasSize(1)))
                .andExpect(jsonPath("$.data.topSellingProducts[0].productName").value(phone.getName()))
                .andExpect(jsonPath("$.data.topSellingProducts[0].quantitySold").value(3))
                .andExpect(jsonPath("$.data.lowStockVariants", hasSize(1)))
                .andExpect(jsonPath("$.data.lowStockVariants[0].sku").value("RPT-PHONE"))
                .andExpect(jsonPath("$.data.lowStockVariants[0].availableQuantity").value(3))
                .andExpect(jsonPath("$.data.lowStockVariants[0].stockStatus").value("LOW_STOCK"));
    }

    @Test
    void productReportValidatesSortAndRequiresAdmin() throws Exception {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        mockMvc.perform(get("/api/v1/admin/reports/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken)
                        .param("fromDate", today.toString())
                        .param("toDate", today.toString()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        mockMvc.perform(get("/api/v1/admin/reports/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .param("fromDate", today.toString())
                        .param("toDate", today.toString())
                        .param("sortBy", "POPULAR"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    private Order saveOrder(String orderNumber, Long variantId, String productName, int quantity,
                            BigDecimal unitPrice, String status) {
        BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
        Order order = new Order(orderNumber, customer, PaymentMethod.COD, subtotal, BigDecimal.ZERO, BigDecimal.ZERO);
        order.addItem(new OrderItem(variantId, productName, orderNumber + "-SKU", null, unitPrice, quantity));
        if (!"PENDING".equals(status)) order.updateStatus(status, null);
        return orders.saveAndFlush(order);
    }

    private User saveUser(String email, String fullName, String phone, Role role) {
        User user = new User(email, "hash", fullName, phone);
        user.addRole(role);
        return users.saveAndFlush(user);
    }
}

package com.techstore.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techstore.entity.Address;
import com.techstore.entity.Brand;
import com.techstore.entity.Category;
import com.techstore.entity.Inventory;
import com.techstore.entity.Product;
import com.techstore.entity.ProductVariant;
import com.techstore.entity.Role;
import com.techstore.entity.User;
import com.techstore.enums.ProductStatus;
import com.techstore.enums.RoleCode;
import com.techstore.enums.VariantStatus;
import com.techstore.repository.AddressRepository;
import com.techstore.repository.BrandRepository;
import com.techstore.repository.CartItemRepository;
import com.techstore.repository.CartRepository;
import com.techstore.repository.CategoryRepository;
import com.techstore.repository.InventoryRepository;
import com.techstore.repository.InventoryTransactionRepository;
import com.techstore.repository.OrderRepository;
import com.techstore.repository.PasswordResetTokenRepository;
import com.techstore.repository.ProductRepository;
import com.techstore.repository.ProductVariantRepository;
import com.techstore.repository.RoleRepository;
import com.techstore.repository.RefreshTokenRepository;
import com.techstore.repository.UserRepository;
import com.techstore.security.TokenIssuer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class CriticalApiIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired DataSource dataSource;
    @Autowired RoleRepository roles;
    @Autowired UserRepository users;
    @Autowired BrandRepository brands;
    @Autowired CategoryRepository categories;
    @Autowired ProductRepository products;
    @Autowired ProductVariantRepository variants;
    @Autowired InventoryRepository inventories;
    @Autowired AddressRepository addresses;
    @Autowired CartRepository carts;
    @Autowired CartItemRepository cartItems;
    @Autowired OrderRepository orders;
    @Autowired InventoryTransactionRepository inventoryTransactions;
    @Autowired RefreshTokenRepository refreshTokens;
    @Autowired PasswordResetTokenRepository passwordResetTokens;
    @Autowired TokenIssuer tokenIssuer;

    private User customer;
    private String customerToken;
    private String adminToken;
    private Brand brand;
    private Category category;
    private Product product;
    private ProductVariant variant;
    private Address address;

    @BeforeEach
    void setUp() {
        cleanDatabase();
        Role customerRole = roles.findByCode(RoleCode.CUSTOMER)
                .orElseGet(() -> roles.save(new Role(RoleCode.CUSTOMER, "Customer")));
        Role adminRole = roles.findByCode(RoleCode.ADMIN)
                .orElseGet(() -> roles.save(new Role(RoleCode.ADMIN, "Admin")));

        customer = new User("us14.customer@example.com", "test-hash", "US14 Customer", "0901234567");
        customer.addRole(customerRole);
        customer = users.saveAndFlush(customer);
        customerToken = tokenIssuer.issue(customer).accessToken();

        User admin = new User("us14.admin@example.com", "test-hash", "US14 Admin", "0907654321");
        admin.addRole(adminRole);
        admin = users.saveAndFlush(admin);
        adminToken = tokenIssuer.issue(admin).accessToken();

        brand = brands.save(new Brand("US14 Brand", "https://example.com/us14.png", "Integration test brand"));
        category = categories.save(new Category("US14 Category", "Integration test category", null, null));
        product = products.save(new Product("US14 Phone", "Critical API product", brand, category, ProductStatus.ACTIVE));
        variant = variants.save(new ProductVariant(product, "US14-SKU-001", "Black", "128GB",
                new BigDecimal("1000000"), new BigDecimal("1200000"), 6, VariantStatus.ACTIVE));
        inventories.save(new Inventory(variant, 6, 0, 2));
        address = addresses.save(new Address(customer, "US14 Customer", "0901234567",
                "Ha Noi", "Cau Giay", "Dich Vong", "1 Duy Tan"));
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
    }

    @Test
    @DisplayName("US-14.2 T-14.2.1: registration and login work through the HTTP and persistence layers")
    void registrationAndLoginFlowReturnsUsableCustomerToken() throws Exception {
        String email = "us14.new.customer@example.com";
        Map<String, String> registration = Map.of(
                "fullName", "New Integration Customer",
                "email", email,
                "phone", "0911111111",
                "password", "strong-password",
                "confirmPassword", "strong-password"
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registration)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", "strong-password"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.user.email").value(email))
                .andReturn();

        String accessToken = responseData(loginResult).path("accessToken").asText();
        mockMvc.perform(get("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(email));
        assertThat(users.findByEmailIgnoreCase(email)).isPresent();
    }

    @Test
    @DisplayName("US-14.2 T-14.2.2: admin creates and reads product, variant and inventory through APIs")
    void productVariantAndInventoryApisPersistConsistentData() throws Exception {
        MvcResult productResult = mockMvc.perform(post("/api/v1/admin/products")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "US14 API Phone",
                                "description", "Created by integration test",
                                "brandId", brand.getId(),
                                "categoryId", category.getId(),
                                "status", "DRAFT"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("US14 API Phone"))
                .andReturn();
        long productId = responseData(productResult).path("id").asLong();

        MvcResult variantResult = mockMvc.perform(post("/api/v1/admin/products/{id}/variants", productId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "sku", "US14-API-SKU",
                                "color", "Blue",
                                "storage", "256GB",
                                "price", 1500000,
                                "originalPrice", 1700000,
                                "stockQuantity", 7,
                                "status", "ACTIVE"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.stockQuantity").value(7))
                .andReturn();
        long variantId = responseData(variantResult).path("id").asLong();

        mockMvc.perform(get("/api/v1/admin/products/{id}", productId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(productId));
        mockMvc.perform(get("/api/v1/admin/products/{id}/variants", productId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id").value(variantId));
        mockMvc.perform(get("/api/v1/admin/inventory/variants/{id}", variantId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.quantityOnHand").value(7))
                .andExpect(jsonPath("$.data.availableQuantity").value(7));
    }

    @Test
    @DisplayName("US-14.2 T-14.2.3: cart to order flow clears cart and deducts inventory atomically")
    void cartToOrderFlowPersistsOrderAndDeductsStock() throws Exception {
        mockMvc.perform(post("/api/v1/cart/items")
                        .header(HttpHeaders.AUTHORIZATION, bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "variantId", variant.getId(),
                                "quantity", 2))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalItems").value(2));

        MvcResult orderResult = mockMvc.perform(post("/api/v1/orders")
                        .header(HttpHeaders.AUTHORIZATION, bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "addressId", address.getId(),
                                "paymentMethod", "COD"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderNumber", startsWith("TS-")))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.items[0].quantity").value(2))
                .andReturn();

        long orderId = responseData(orderResult).path("id").asLong();
        assertThat(orders.findById(orderId)).isPresent();
        assertThat(inventories.findByVariantId(variant.getId()).orElseThrow().getQuantityOnHand()).isEqualTo(4);
        assertThat(carts.findByUserId(customer.getId())).isPresent();
        assertThat(cartItems.findByCartId(carts.findByUserId(customer.getId()).orElseThrow().getId())).isEmpty();
    }

    @Test
    @DisplayName("US-14.2: invalid data and insufficient permissions are rejected without mutation")
    void invalidDataAndCustomerAdminAccessAreRejected() throws Exception {
        long productCount = products.count();

        mockMvc.perform(post("/api/v1/cart/items")
                        .header(HttpHeaders.AUTHORIZATION, bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"variantId\":" + variant.getId() + ",\"quantity\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        mockMvc.perform(post("/api/v1/admin/products")
                        .header(HttpHeaders.AUTHORIZATION, bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"addressId\":" + address.getId() + ",\"paymentMethod\":\"COD\"}"))
                .andExpect(status().isUnauthorized());

        assertThat(products.count()).isEqualTo(productCount);
        assertThat(orders.count()).isZero();
    }

    @Test
    @DisplayName("US-14.2 T-14.2.4: integration tests run against an isolated temporary H2 database")
    void usesIsolatedInMemoryTestDatabase() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            assertThat(connection.getMetaData().getURL()).startsWith("jdbc:h2:mem:");
            assertThat(connection.getMetaData().getDatabaseProductName()).isEqualTo("H2");
        }
    }

    private JsonNode responseData(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private void cleanDatabase() {
        inventoryTransactions.deleteAll();
        orders.deleteAll();
        cartItems.deleteAll();
        carts.deleteAll();
        addresses.deleteAll();
        inventories.deleteAll();
        variants.deleteAll();
        products.deleteAll();
        categories.deleteAll();
        brands.deleteAll();
        passwordResetTokens.deleteAll();
        refreshTokens.deleteAll();
        users.deleteAll();
    }
}

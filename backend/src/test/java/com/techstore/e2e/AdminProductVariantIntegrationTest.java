package com.techstore.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techstore.dto.request.ProductVariantRequest;
import com.techstore.entity.Brand;
import com.techstore.entity.Category;
import com.techstore.entity.Product;
import com.techstore.entity.ProductVariant;
import com.techstore.entity.Role;
import com.techstore.entity.User;
import com.techstore.enums.ProductStatus;
import com.techstore.enums.RoleCode;
import com.techstore.enums.VariantStatus;
import com.techstore.repository.BrandRepository;
import com.techstore.repository.CategoryRepository;
import com.techstore.repository.PasswordResetTokenRepository;
import com.techstore.repository.ProductImageRepository;
import com.techstore.repository.ProductRepository;
import com.techstore.repository.ProductSpecificationRepository;
import com.techstore.repository.ProductVariantRepository;
import com.techstore.repository.RefreshTokenRepository;
import com.techstore.repository.RoleRepository;
import com.techstore.repository.UserRepository;
import com.techstore.security.IssuedTokenPair;
import com.techstore.security.TokenIssuer;
import com.techstore.service.impl.ProductVariantServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class AdminProductVariantIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private com.techstore.repository.InventoryTransactionRepository inventoryTransactionRepository;

    @Autowired
    private com.techstore.repository.InventoryRepository inventoryRepository;

    @Autowired
    private ProductSpecificationRepository productSpecificationRepository;

    @Autowired
    private ProductVariantRepository productVariantRepository;

    @Autowired
    private ProductImageRepository productImageRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TokenIssuer tokenIssuer;

    @Autowired
    private ProductVariantServiceImpl productVariantService;

    private String adminToken;
    private String customerToken;
    private Product testProduct;
    private Product secondProduct;

    @BeforeEach
    void setUp() {
        // Reset orderChecker to default
        productVariantService.setOrderChecker(id -> false);
        cleanDatabase();

        Role adminRole = roleRepository.findByCode(RoleCode.ADMIN)
                .orElseGet(() -> roleRepository.save(new Role(RoleCode.ADMIN, "Quản trị viên")));
        Role customerRole = roleRepository.findByCode(RoleCode.CUSTOMER)
                .orElseGet(() -> roleRepository.save(new Role(RoleCode.CUSTOMER, "Khách hàng")));

        User admin = new User("admin@techstore.com", passwordEncoder.encode("Admin@123"), "Admin User", "0900000001");
        admin.addRole(adminRole);
        admin = userRepository.save(admin);

        User customer = new User("customer@techstore.com", passwordEncoder.encode("Customer@123"), "Customer User", "0900000002");
        customer.addRole(customerRole);
        customer = userRepository.save(customer);

        IssuedTokenPair adminTokens = tokenIssuer.issue(admin);
        adminToken = adminTokens.accessToken();

        IssuedTokenPair customerTokens = tokenIssuer.issue(customer);
        customerToken = customerTokens.accessToken();

        Brand apple = brandRepository.save(new Brand("Apple", "https://example.com/apple.png", "Apple Inc"));
        Category phone = categoryRepository.save(new Category("Điện thoại", "Smartphone", null, null));

        testProduct = productRepository.save(new Product("iPhone 16 Pro", "Flagship", apple, phone, ProductStatus.DRAFT));
        secondProduct = productRepository.save(new Product("iPhone 16", "Standard", apple, phone, ProductStatus.DRAFT));
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
    }

    private void cleanDatabase() {
        inventoryTransactionRepository.deleteAll();
        inventoryRepository.deleteAll();
        productSpecificationRepository.deleteAll();
        productImageRepository.deleteAll();
        productVariantRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        brandRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        passwordResetTokenRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();
    }

    @Test
    @DisplayName("Admin tạo biến thể mới cho sản phẩm thành công")
    void createVariant_asAdmin_success() throws Exception {
        ProductVariantRequest request = new ProductVariantRequest(
                "IP16P-256-DESERT",
                "Sa mạc titan",
                "256GB",
                new BigDecimal("28990000.00"),
                new BigDecimal("31990000.00"),
                50,
                VariantStatus.ACTIVE
        );

        mockMvc.perform(post("/api/v1/admin/products/" + testProduct.getId() + "/variants")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sku").value("IP16P-256-DESERT"))
                .andExpect(jsonPath("$.data.color").value("Sa mạc titan"))
                .andExpect(jsonPath("$.data.storage").value("256GB"))
                .andExpect(jsonPath("$.data.price").value(28990000.00))
                .andExpect(jsonPath("$.data.originalPrice").value(31990000.00))
                .andExpect(jsonPath("$.data.stockQuantity").value(50))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.productId").value(testProduct.getId()));

        assertThat(productVariantRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("Chặn tạo biến thể với SKU đã tồn tại trong toàn hệ thống (400 Bad Request)")
    void createVariant_duplicateSku_throwsBadRequest() throws Exception {
        productVariantRepository.save(new ProductVariant(
                testProduct,
                "IP16-SKU-001",
                "Đen",
                "128GB",
                new BigDecimal("20000000.00"),
                null,
                10,
                VariantStatus.ACTIVE
        ));

        // Thử tạo biến thể trên sản phẩm khác nhưng cùng SKU
        ProductVariantRequest duplicate = new ProductVariantRequest(
                "ip16-sku-001",
                "Trắng",
                "256GB",
                new BigDecimal("22000000.00"),
                null,
                10,
                VariantStatus.ACTIVE
        );

        mockMvc.perform(post("/api/v1/admin/products/" + secondProduct.getId() + "/variants")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicate)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VARIANT_SKU_DUPLICATE"));
    }

    @Test
    @DisplayName("Chặn tạo biến thể khi giá gốc nhỏ hơn giá bán (400 Bad Request)")
    void createVariant_originalPriceLessThanPrice_throwsBadRequest() throws Exception {
        ProductVariantRequest request = new ProductVariantRequest(
                "IP16-INVALID-PRICE",
                "Xanh",
                "128GB",
                new BigDecimal("25000000.00"),
                new BigDecimal("20000000.00"), // originalPrice < price
                10,
                VariantStatus.ACTIVE
        );

        mockMvc.perform(post("/api/v1/admin/products/" + testProduct.getId() + "/variants")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VARIANT_INVALID_PRICE"))
                .andExpect(jsonPath("$.message").value("Giá gốc phải lớn hơn hoặc bằng giá bán"));
    }

    @Test
    @DisplayName("Chặn tạo biến thể khi giá bán âm (400 Bad Request)")
    void createVariant_negativePrice_throwsBadRequest() throws Exception {
        ProductVariantRequest request = new ProductVariantRequest(
                "IP16-NEG-PRICE",
                "Đen",
                "128GB",
                new BigDecimal("-5000.00"),
                null,
                10,
                VariantStatus.ACTIVE
        );

        mockMvc.perform(post("/api/v1/admin/products/" + testProduct.getId() + "/variants")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Chặn tạo biến thể khi sản phẩm không tồn tại (404 Not Found)")
    void createVariant_productNotFound_throwsNotFound() throws Exception {
        ProductVariantRequest request = new ProductVariantRequest(
                "IP16-VALID-SKU",
                "Đen",
                "128GB",
                new BigDecimal("20000000.00"),
                null,
                10,
                VariantStatus.ACTIVE
        );

        mockMvc.perform(post("/api/v1/admin/products/999999/variants")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));
    }

    @Test
    @DisplayName("Admin lấy danh sách biến thể của sản phẩm thành công")
    void getVariants_asAdmin_returnsList() throws Exception {
        productVariantRepository.save(new ProductVariant(
                testProduct, "SKU-1", "Đen", "128GB", new BigDecimal("20000000"), null, 5, VariantStatus.ACTIVE
        ));
        productVariantRepository.save(new ProductVariant(
                testProduct, "SKU-2", "Trắng", "256GB", new BigDecimal("23000000"), null, 10, VariantStatus.ACTIVE
        ));

        mockMvc.perform(get("/api/v1/admin/products/" + testProduct.getId() + "/variants")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].sku").value("SKU-1"))
                .andExpect(jsonPath("$.data[1].sku").value("SKU-2"));
    }

    @Test
    @DisplayName("Admin lấy chi tiết một biến thể thành công")
    void getVariantById_asAdmin_success() throws Exception {
        ProductVariant saved = productVariantRepository.save(new ProductVariant(
                testProduct, "SKU-DETAIL", "Titan Tự Nhiên", "512GB", new BigDecimal("35000000"), null, 8, VariantStatus.ACTIVE
        ));

        mockMvc.perform(get("/api/v1/admin/products/" + testProduct.getId() + "/variants/" + saved.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(saved.getId()))
                .andExpect(jsonPath("$.data.sku").value("SKU-DETAIL"))
                .andExpect(jsonPath("$.data.color").value("Titan Tự Nhiên"));
    }

    @Test
    @DisplayName("Admin cập nhật biến thể thành công")
    void updateVariant_asAdmin_success() throws Exception {
        ProductVariant saved = productVariantRepository.save(new ProductVariant(
                testProduct, "SKU-ORIGINAL", "Đen", "128GB", new BigDecimal("20000000"), null, 5, VariantStatus.ACTIVE
        ));

        ProductVariantRequest updateRequest = new ProductVariantRequest(
                "SKU-UPDATED",
                "Đen Bóng",
                "256GB",
                new BigDecimal("22500000.00"),
                new BigDecimal("25000000.00"),
                20,
                VariantStatus.INACTIVE
        );

        mockMvc.perform(put("/api/v1/admin/products/" + testProduct.getId() + "/variants/" + saved.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sku").value("SKU-UPDATED"))
                .andExpect(jsonPath("$.data.color").value("Đen Bóng"))
                .andExpect(jsonPath("$.data.storage").value("256GB"))
                .andExpect(jsonPath("$.data.price").value(22500000.00))
                .andExpect(jsonPath("$.data.status").value("INACTIVE"));
    }

    @Test
    @DisplayName("Chặn cập nhật biến thể với SKU trùng với biến thể khác")
    void updateVariant_duplicateSkuFromAnotherVariant_throwsBadRequest() throws Exception {
        productVariantRepository.save(new ProductVariant(
                testProduct, "SKU-EXISTS", "Đỏ", "128GB", new BigDecimal("20000000"), null, 5, VariantStatus.ACTIVE
        ));
        ProductVariant target = productVariantRepository.save(new ProductVariant(
                testProduct, "SKU-TO-EDIT", "Xanh", "128GB", new BigDecimal("20000000"), null, 5, VariantStatus.ACTIVE
        ));

        ProductVariantRequest conflict = new ProductVariantRequest(
                "SKU-EXISTS",
                "Xanh",
                "128GB",
                new BigDecimal("20000000"),
                null,
                5,
                VariantStatus.ACTIVE
        );

        mockMvc.perform(put("/api/v1/admin/products/" + testProduct.getId() + "/variants/" + target.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(conflict)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VARIANT_SKU_DUPLICATE"));
    }

    @Test
    @DisplayName("Admin xoá biến thể thành công khi chưa phát sinh đơn hàng")
    void deleteVariant_asAdmin_success() throws Exception {
        ProductVariant saved = productVariantRepository.save(new ProductVariant(
                testProduct, "SKU-TO-DELETE", "Vàng", "64GB", new BigDecimal("15000000"), null, 3, VariantStatus.ACTIVE
        ));

        mockMvc.perform(delete("/api/v1/admin/products/" + testProduct.getId() + "/variants/" + saved.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Xoá biến thể sản phẩm thành công"));

        ProductVariant deleted = productVariantRepository.findById(saved.getId()).orElseThrow();
        assertThat(deleted.isDeleted()).isTrue();
        assertThat(deleted.getDeletedAt()).isNotNull();
        assertThat(productVariantRepository.findByIdAndProductIdAndIsDeletedFalse(saved.getId(), testProduct.getId())).isEmpty();
    }

    @Test
    @DisplayName("Chặn xoá biến thể khi đã phát sinh đơn hàng (400 Bad Request)")
    void deleteVariant_havingOrders_throwsBadRequest() throws Exception {
        ProductVariant saved = productVariantRepository.save(new ProductVariant(
                testProduct, "SKU-ORDERED", "Titan Sa Mạc", "1TB", new BigDecimal("45000000"), null, 2, VariantStatus.ACTIVE
        ));

        // Mô phỏng biến thể đã có đơn hàng
        productVariantService.setOrderChecker(id -> id.equals(saved.getId()));

        mockMvc.perform(delete("/api/v1/admin/products/" + testProduct.getId() + "/variants/" + saved.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VARIANT_HAS_ORDERS"))
                .andExpect(jsonPath("$.message").value("Không thể xoá biến thể đã phát sinh đơn hàng, vui lòng chuyển trạng thái sang ngừng bán"));

        assertThat(productVariantRepository.findById(saved.getId())).isPresent();
    }

    @Test
    @DisplayName("Customer truy cập API biến thể sản phẩm bị cấm (403 Forbidden)")
    void variantApi_asCustomer_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/products/" + testProduct.getId() + "/variants")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Khách vãng lai truy cập API biến thể sản phẩm bị từ chối (401 Unauthorized)")
    void variantApi_unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/admin/products/" + testProduct.getId() + "/variants"))
                .andExpect(status().isUnauthorized());
    }
}

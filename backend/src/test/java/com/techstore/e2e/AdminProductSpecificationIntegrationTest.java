package com.techstore.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techstore.dto.request.ProductSpecificationRequest;
import com.techstore.entity.Brand;
import com.techstore.entity.Category;
import com.techstore.entity.Product;
import com.techstore.entity.ProductSpecification;
import com.techstore.entity.Role;
import com.techstore.entity.User;
import com.techstore.enums.ProductStatus;
import com.techstore.enums.RoleCode;
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
class AdminProductSpecificationIntegrationTest {

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
    private ProductImageRepository productImageRepository;

    @Autowired
    private ProductVariantRepository productVariantRepository;

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

    private String adminToken;
    private String customerToken;
    private Product testProduct;
    private Product secondProduct;

    @BeforeEach
    void setUp() {
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
        secondProduct = productRepository.save(new Product("Galaxy S25 Ultra", "Samsung Flagship", apple, phone, ProductStatus.DRAFT));
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
    @DisplayName("Admin tạo mới thông số kỹ thuật cho sản phẩm thành công")
    void createSpecification_asAdmin_success() throws Exception {
        ProductSpecificationRequest request = new ProductSpecificationRequest(
                "CPU",
                "Apple A18 Pro 6 nhân",
                1
        );

        mockMvc.perform(post("/api/v1/admin/products/" + testProduct.getId() + "/specifications")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.specKey").value("CPU"))
                .andExpect(jsonPath("$.data.specValue").value("Apple A18 Pro 6 nhân"))
                .andExpect(jsonPath("$.data.displayOrder").value(1))
                .andExpect(jsonPath("$.data.productId").value(testProduct.getId()));

        assertThat(productSpecificationRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("Chặn tạo thông số kỹ thuật trùng tên trên cùng một sản phẩm (400 Bad Request)")
    void createSpecification_duplicateKey_throwsBadRequest() throws Exception {
        productSpecificationRepository.save(new ProductSpecification(testProduct, "CPU", "A18 Pro", 1));

        ProductSpecificationRequest duplicateRequest = new ProductSpecificationRequest(
                "cpu", // case-insensitive check
                "A18 Pro Max",
                2
        );

        mockMvc.perform(post("/api/v1/admin/products/" + testProduct.getId() + "/specifications")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SPECIFICATION_KEY_DUPLICATE"))
                .andExpect(jsonPath("$.message").value("Thông số kỹ thuật 'cpu' đã tồn tại cho sản phẩm này"));
    }

    @Test
    @DisplayName("Cho phép tạo thông số kỹ thuật cùng tên trên hai sản phẩm khác nhau")
    void createSpecification_sameKeyOnDifferentProducts_success() throws Exception {
        productSpecificationRepository.save(new ProductSpecification(testProduct, "CPU", "Apple A18 Pro", 1));

        ProductSpecificationRequest requestForSecond = new ProductSpecificationRequest(
                "CPU",
                "Snapdragon 8 Elite",
                1
        );

        mockMvc.perform(post("/api/v1/admin/products/" + secondProduct.getId() + "/specifications")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestForSecond)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.specKey").value("CPU"))
                .andExpect(jsonPath("$.data.productId").value(secondProduct.getId()));
    }

    @Test
    @DisplayName("Chặn tạo thông số kỹ thuật khi tên hoặc giá trị để trống (400 Bad Request)")
    void createSpecification_blankFields_throwsBadRequest() throws Exception {
        ProductSpecificationRequest invalid = new ProductSpecificationRequest("", "", 1);

        mockMvc.perform(post("/api/v1/admin/products/" + testProduct.getId() + "/specifications")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("Chặn tạo thông số kỹ thuật khi sản phẩm không tồn tại (404 Not Found)")
    void createSpecification_productNotFound_throwsNotFound() throws Exception {
        ProductSpecificationRequest request = new ProductSpecificationRequest("RAM", "8GB", 1);

        mockMvc.perform(post("/api/v1/admin/products/999999/specifications")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));
    }

    @Test
    @DisplayName("Chặn tạo thông số kỹ thuật trên sản phẩm đã xoá mềm (404 Not Found)")
    void createSpecification_onSoftDeletedProduct_throwsNotFound() throws Exception {
        testProduct.softDelete();
        productRepository.save(testProduct);

        ProductSpecificationRequest request = new ProductSpecificationRequest("RAM", "8GB", 1);

        mockMvc.perform(post("/api/v1/admin/products/" + testProduct.getId() + "/specifications")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));
    }

    @Test
    @DisplayName("Admin lấy danh sách thông số kỹ thuật đúng thứ tự displayOrder")
    void getSpecifications_asAdmin_returnsOrderedList() throws Exception {
        productSpecificationRepository.save(new ProductSpecification(testProduct, "Pin", "4422 mAh", 3));
        productSpecificationRepository.save(new ProductSpecification(testProduct, "Màn hình", "6.3 inch OLED", 1));
        productSpecificationRepository.save(new ProductSpecification(testProduct, "RAM", "8GB", 2));

        mockMvc.perform(get("/api/v1/admin/products/" + testProduct.getId() + "/specifications")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(3)))
                .andExpect(jsonPath("$.data[0].specKey").value("Màn hình"))
                .andExpect(jsonPath("$.data[1].specKey").value("RAM"))
                .andExpect(jsonPath("$.data[2].specKey").value("Pin"));
    }

    @Test
    @DisplayName("Admin lấy chi tiết một thông số kỹ thuật theo ID thành công")
    void getSpecificationById_asAdmin_success() throws Exception {
        ProductSpecification saved = productSpecificationRepository.save(
                new ProductSpecification(testProduct, "Camera", "48MP Fusion", 1)
        );

        mockMvc.perform(get("/api/v1/admin/products/" + testProduct.getId() + "/specifications/" + saved.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(saved.getId()))
                .andExpect(jsonPath("$.data.specKey").value("Camera"))
                .andExpect(jsonPath("$.data.specValue").value("48MP Fusion"));
    }

    @Test
    @DisplayName("Admin cập nhật thông số kỹ thuật thành công")
    void updateSpecification_asAdmin_success() throws Exception {
        ProductSpecification saved = productSpecificationRepository.save(
                new ProductSpecification(testProduct, "ROM", "128GB", 1)
        );

        ProductSpecificationRequest update = new ProductSpecificationRequest(
                "Bộ nhớ trong",
                "256GB NVMe",
                5
        );

        mockMvc.perform(put("/api/v1/admin/products/" + testProduct.getId() + "/specifications/" + saved.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.specKey").value("Bộ nhớ trong"))
                .andExpect(jsonPath("$.data.specValue").value("256GB NVMe"))
                .andExpect(jsonPath("$.data.displayOrder").value(5));

        ProductSpecification updated = productSpecificationRepository.findById(saved.getId()).orElseThrow();
        assertThat(updated.getSpecKey()).isEqualTo("Bộ nhớ trong");
        assertThat(updated.getSpecValue()).isEqualTo("256GB NVMe");
        assertThat(updated.getDisplayOrder()).isEqualTo(5);
    }

    @Test
    @DisplayName("Chặn cập nhật thông số trùng tên với thông số khác cùng sản phẩm (400 Bad Request)")
    void updateSpecification_duplicateKey_throwsBadRequest() throws Exception {
        productSpecificationRepository.save(new ProductSpecification(testProduct, "Màn hình", "OLED", 1));
        ProductSpecification target = productSpecificationRepository.save(
                new ProductSpecification(testProduct, "Display", "Super Retina", 2)
        );

        ProductSpecificationRequest conflict = new ProductSpecificationRequest("màn hình", "OLED 120Hz", 2);

        mockMvc.perform(put("/api/v1/admin/products/" + testProduct.getId() + "/specifications/" + target.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(conflict)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SPECIFICATION_KEY_DUPLICATE"));
    }

    @Test
    @DisplayName("Admin xoá thông số kỹ thuật thành công")
    void deleteSpecification_asAdmin_success() throws Exception {
        ProductSpecification saved = productSpecificationRepository.save(
                new ProductSpecification(testProduct, "Cổng kết nối", "USB-C 3.0", 1)
        );

        mockMvc.perform(delete("/api/v1/admin/products/" + testProduct.getId() + "/specifications/" + saved.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Xoá thông số kỹ thuật thành công"));

        assertThat(productSpecificationRepository.findById(saved.getId())).isEmpty();
    }

    @Test
    @DisplayName("Customer truy cập API thông số sản phẩm bị cấm (403 Forbidden)")
    void specificationApi_asCustomer_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/products/" + testProduct.getId() + "/specifications")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Khách vãng lai truy cập API thông số bị từ chối (401 Unauthorized)")
    void specificationApi_unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/admin/products/" + testProduct.getId() + "/specifications"))
                .andExpect(status().isUnauthorized());
    }
}

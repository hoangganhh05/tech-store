package com.techstore.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techstore.dto.request.ProductCreateRequest;
import com.techstore.dto.request.ProductStatusUpdateRequest;
import com.techstore.dto.request.ProductUpdateRequest;
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
import com.techstore.repository.ProductVariantRepository;
import com.techstore.repository.RefreshTokenRepository;
import com.techstore.repository.RoleRepository;
import com.techstore.repository.UserRepository;
import com.techstore.security.IssuedTokenPair;
import com.techstore.security.TokenIssuer;
import com.techstore.service.impl.ProductServiceImpl;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class AdminProductIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductVariantRepository productVariantRepository;

    @Autowired
    private ProductImageRepository productImageRepository;

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
    private ProductServiceImpl productServiceImpl;

    private String adminToken;
    private String customerToken;
    private Brand appleBrand;
    private Brand samsungBrand;
    private Category phoneCategory;

    @BeforeEach
    void setUp() {
        productServiceImpl.setOrderChecker(id -> false);
        productImageRepository.deleteAll();
        productVariantRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        brandRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        passwordResetTokenRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();

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

        appleBrand = brandRepository.save(new Brand("Apple", "https://example.com/apple.png", "Hãng Apple"));
        samsungBrand = brandRepository.save(new Brand("Samsung", "https://example.com/samsung.png", "Hãng Samsung"));
        phoneCategory = categoryRepository.save(new Category("Điện thoại", "Điện thoại thông minh", null, null));
    }

    @Test
    @DisplayName("Admin tạo sản phẩm mới với thông tin cơ bản thành công (mặc định DRAFT)")
    void createProduct_asAdmin_success() throws Exception {
        ProductCreateRequest request = new ProductCreateRequest(
                "iPhone 16 Pro Max",
                "Flagship mới nhất từ Apple",
                appleBrand.getId(),
                phoneCategory.getId(),
                ProductStatus.DRAFT
        );

        mockMvc.perform(post("/api/v1/admin/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("iPhone 16 Pro Max"))
                .andExpect(jsonPath("$.data.description").value("Flagship mới nhất từ Apple"))
                .andExpect(jsonPath("$.data.brandId").value(appleBrand.getId()))
                .andExpect(jsonPath("$.data.brandName").value("Apple"))
                .andExpect(jsonPath("$.data.categoryId").value(phoneCategory.getId()))
                .andExpect(jsonPath("$.data.categoryName").value("Điện thoại"))
                .andExpect(jsonPath("$.data.status").value("DRAFT"));

        assertThat(productRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("Chặn tạo sản phẩm trùng tên trong cùng một thương hiệu (400 Bad Request)")
    void createProduct_duplicateNameInSameBrand_throwsBadRequest() throws Exception {
        productRepository.save(new Product("iPhone 16", null, appleBrand, phoneCategory, ProductStatus.DRAFT));

        ProductCreateRequest duplicate = new ProductCreateRequest(
                "iPhone 16",
                "Mô tả khác",
                appleBrand.getId(),
                phoneCategory.getId(),
                ProductStatus.DRAFT
        );

        mockMvc.perform(post("/api/v1/admin/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicate)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Tên sản phẩm đã tồn tại trong cùng thương hiệu"));
    }

    @Test
    @DisplayName("Cho phép tạo sản phẩm cùng tên nhưng ở thương hiệu khác nhau")
    void createProduct_sameNameDifferentBrand_success() throws Exception {
        productRepository.save(new Product("Buds Pro", null, appleBrand, phoneCategory, ProductStatus.DRAFT));

        ProductCreateRequest request = new ProductCreateRequest(
                "Buds Pro",
                "Tai nghe Samsung",
                samsungBrand.getId(),
                phoneCategory.getId(),
                ProductStatus.DRAFT
        );

        mockMvc.perform(post("/api/v1/admin/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Buds Pro"))
                .andExpect(jsonPath("$.data.brandName").value("Samsung"));

        assertThat(productRepository.findAll()).hasSize(2);
    }

    @Test
    @DisplayName("Chặn tạo sản phẩm khi thương hiệu không tồn tại (404 Not Found)")
    void createProduct_brandNotFound_throwsNotFound() throws Exception {
        ProductCreateRequest request = new ProductCreateRequest(
                "Test Phone",
                "Mô tả",
                999999L,
                phoneCategory.getId(),
                ProductStatus.DRAFT
        );

        mockMvc.perform(post("/api/v1/admin/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Không tìm thấy thương hiệu với ID: 999999"));
    }

    @Test
    @DisplayName("Chặn tạo sản phẩm khi danh mục không tồn tại (404 Not Found)")
    void createProduct_categoryNotFound_throwsNotFound() throws Exception {
        ProductCreateRequest request = new ProductCreateRequest(
                "Test Phone",
                "Mô tả",
                appleBrand.getId(),
                999999L,
                ProductStatus.DRAFT
        );

        mockMvc.perform(post("/api/v1/admin/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Không tìm thấy danh mục với ID: 999999"));
    }

    @Test
    @DisplayName("Chặn tạo sản phẩm với trạng thái ACTIVE khi chưa có biến thể (400 Bad Request)")
    void createProduct_withActiveStatusWithoutVariants_throwsBadRequest() throws Exception {
        ProductCreateRequest request = new ProductCreateRequest(
                "iPhone 16 Pro",
                "Mô tả",
                appleBrand.getId(),
                phoneCategory.getId(),
                ProductStatus.ACTIVE
        );

        mockMvc.perform(post("/api/v1/admin/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Sản phẩm mới tạo phải ở trạng thái nháp (DRAFT), chỉ có thể chuyển sang đang bán khi có ít nhất một biến thể hợp lệ"));
    }

    @Test
    @DisplayName("Chặn tạo sản phẩm khi thiếu các trường bắt buộc")
    void createProduct_missingRequiredFields_throwsBadRequest() throws Exception {
        ProductCreateRequest request = new ProductCreateRequest(
                "",
                null,
                null,
                null,
                null
        );

        mockMvc.perform(post("/api/v1/admin/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Admin cập nhật thông tin sản phẩm thành công")
    void updateProduct_asAdmin_success() throws Exception {
        Product product = productRepository.save(new Product(
                "iPhone 15",
                "Mô tả cũ",
                appleBrand,
                phoneCategory,
                ProductStatus.DRAFT
        ));

        ProductUpdateRequest updateRequest = new ProductUpdateRequest(
                "iPhone 15 Pro",
                "Mô tả mới cập nhật",
                appleBrand.getId(),
                phoneCategory.getId(),
                ProductStatus.DRAFT
        );

        mockMvc.perform(put("/api/v1/admin/products/" + product.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(product.getId()))
                .andExpect(jsonPath("$.data.name").value("iPhone 15 Pro"))
                .andExpect(jsonPath("$.data.description").value("Mô tả mới cập nhật"))
                .andExpect(jsonPath("$.data.brandId").value(appleBrand.getId()))
                .andExpect(jsonPath("$.data.status").value("DRAFT"));

        Product updated = productRepository.findById(product.getId()).orElseThrow();
        assertThat(updated.getName()).isEqualTo("iPhone 15 Pro");
        assertThat(updated.getDescription()).isEqualTo("Mô tả mới cập nhật");
    }

    @Test
    @DisplayName("Admin cập nhật sản phẩm giữ nguyên tên của chính nó thành công (không bị lỗi trùng lặp)")
    void updateProduct_keepSameName_success() throws Exception {
        Product product = productRepository.save(new Product(
                "iPhone 15",
                "Mô tả ban đầu",
                appleBrand,
                phoneCategory,
                ProductStatus.DRAFT
        ));

        ProductUpdateRequest updateRequest = new ProductUpdateRequest(
                "iPhone 15",
                "Mô tả đã sửa đổi",
                appleBrand.getId(),
                phoneCategory.getId(),
                ProductStatus.DRAFT
        );

        mockMvc.perform(put("/api/v1/admin/products/" + product.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("iPhone 15"))
                .andExpect(jsonPath("$.data.description").value("Mô tả đã sửa đổi"));
    }

    @Test
    @DisplayName("Chặn cập nhật sản phẩm trùng tên với sản phẩm khác trong cùng thương hiệu (400 Bad Request)")
    void updateProduct_duplicateNameInSameBrand_throwsBadRequest() throws Exception {
        productRepository.save(new Product("iPhone 15", null, appleBrand, phoneCategory, ProductStatus.DRAFT));
        Product product2 = productRepository.save(new Product("iPhone 16", null, appleBrand, phoneCategory, ProductStatus.DRAFT));

        ProductUpdateRequest updateRequest = new ProductUpdateRequest(
                "iPhone 15",
                "Đổi tên sang iPhone 15 nhưng đã có sản phẩm khác tên này",
                appleBrand.getId(),
                phoneCategory.getId(),
                ProductStatus.DRAFT
        );

        mockMvc.perform(put("/api/v1/admin/products/" + product2.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Tên sản phẩm đã tồn tại trong cùng thương hiệu"));
    }

    @Test
    @DisplayName("Chặn cập nhật trạng thái sản phẩm sang ACTIVE khi chưa có biến thể nào")
    void updateProduct_changeStatusToActiveWithoutVariants_throwsBadRequest() throws Exception {
        Product product = productRepository.save(new Product("iPhone 15", null, appleBrand, phoneCategory, ProductStatus.DRAFT));

        ProductUpdateRequest updateRequest = new ProductUpdateRequest(
                "iPhone 15",
                "Chuyển sang ACTIVE",
                appleBrand.getId(),
                phoneCategory.getId(),
                ProductStatus.ACTIVE
        );

        mockMvc.perform(put("/api/v1/admin/products/" + product.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Sản phẩm chỉ có thể chuyển sang đang bán khi có ít nhất một biến thể hợp lệ"));
    }

    @Test
    @DisplayName("Cho phép cập nhật trạng thái sản phẩm sang ACTIVE khi đã có biến thể hợp lệ")
    void updateProduct_changeStatusToActiveWithVariants_success() throws Exception {
        Product product = productRepository.save(new Product("iPhone 15", null, appleBrand, phoneCategory, ProductStatus.DRAFT));
        productVariantRepository.save(new ProductVariant(
                product,
                "IP15-BLK-128",
                "Đen",
                "128GB",
                BigDecimal.valueOf(20000000),
                BigDecimal.valueOf(22000000),
                10,
                VariantStatus.ACTIVE
        ));

        ProductUpdateRequest updateRequest = new ProductUpdateRequest(
                "iPhone 15",
                "Chuyển sang ACTIVE khi đã có biến thể",
                appleBrand.getId(),
                phoneCategory.getId(),
                ProductStatus.ACTIVE
        );

        mockMvc.perform(put("/api/v1/admin/products/" + product.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("Chặn cập nhật sản phẩm không tồn tại (404 Not Found)")
    void updateProduct_notFound_throwsNotFound() throws Exception {
        ProductUpdateRequest updateRequest = new ProductUpdateRequest(
                "Test Non-existent",
                "Mô tả",
                appleBrand.getId(),
                phoneCategory.getId(),
                ProductStatus.DRAFT
        );

        mockMvc.perform(put("/api/v1/admin/products/999999")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Không tìm thấy sản phẩm với ID: 999999"));
    }

    // --- PATCH Status Endpoints Tests ---

    @Test
    @DisplayName("Admin cập nhật trạng thái nhanh sang ACTIVE khi có biến thể thành công")
    void updateProductStatus_asAdmin_toActive_withVariants_success() throws Exception {
        Product product = productRepository.save(new Product("iPhone 15", null, appleBrand, phoneCategory, ProductStatus.DRAFT));
        productVariantRepository.save(new ProductVariant(
                product,
                "IP15-STATUS-TEST",
                "Xanh",
                "128GB",
                BigDecimal.valueOf(20000000),
                BigDecimal.valueOf(22000000),
                10,
                VariantStatus.ACTIVE
        ));

        ProductStatusUpdateRequest statusRequest = new ProductStatusUpdateRequest(ProductStatus.ACTIVE);

        mockMvc.perform(patch("/api/v1/admin/products/" + product.getId() + "/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        Product updated = productRepository.findById(product.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(ProductStatus.ACTIVE);
    }

    @Test
    @DisplayName("Chặn cập nhật trạng thái nhanh sang ACTIVE khi chưa có biến thể (400 Bad Request)")
    void updateProductStatus_toActive_withoutVariants_throwsBadRequest() throws Exception {
        Product product = productRepository.save(new Product("iPhone 15", null, appleBrand, phoneCategory, ProductStatus.DRAFT));

        ProductStatusUpdateRequest statusRequest = new ProductStatusUpdateRequest(ProductStatus.ACTIVE);

        mockMvc.perform(patch("/api/v1/admin/products/" + product.getId() + "/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Sản phẩm chỉ có thể chuyển sang đang bán khi có ít nhất một biến thể hợp lệ"));
    }

    @Test
    @DisplayName("Admin cập nhật trạng thái nhanh sang INACTIVE (Ngừng bán) thành công")
    void updateProductStatus_asAdmin_toInactive_success() throws Exception {
        Product product = productRepository.save(new Product("iPhone 15", null, appleBrand, phoneCategory, ProductStatus.ACTIVE));

        ProductStatusUpdateRequest statusRequest = new ProductStatusUpdateRequest(ProductStatus.INACTIVE);

        mockMvc.perform(patch("/api/v1/admin/products/" + product.getId() + "/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("INACTIVE"));

        Product updated = productRepository.findById(product.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(ProductStatus.INACTIVE);
    }

    @Test
    @DisplayName("Admin cập nhật trạng thái nhanh sang DRAFT (Nháp) thành công")
    void updateProductStatus_asAdmin_toDraft_success() throws Exception {
        Product product = productRepository.save(new Product("iPhone 15", null, appleBrand, phoneCategory, ProductStatus.INACTIVE));

        ProductStatusUpdateRequest statusRequest = new ProductStatusUpdateRequest(ProductStatus.DRAFT);

        mockMvc.perform(patch("/api/v1/admin/products/" + product.getId() + "/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DRAFT"));

        Product updated = productRepository.findById(product.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(ProductStatus.DRAFT);
    }

    @Test
    @DisplayName("Chặn cập nhật trạng thái nhanh khi sản phẩm không tồn tại (404 Not Found)")
    void updateProductStatus_notFound_throwsNotFound() throws Exception {
        ProductStatusUpdateRequest statusRequest = new ProductStatusUpdateRequest(ProductStatus.ACTIVE);

        mockMvc.perform(patch("/api/v1/admin/products/999999/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Không tìm thấy sản phẩm với ID: 999999"));
    }

    // --- DELETE Product (Soft Delete) Endpoints Tests ---

    @Test
    @DisplayName("Admin xoá mềm sản phẩm chưa phát sinh đơn hàng thành công")
    void deleteProduct_asAdmin_success() throws Exception {
        Product product = productRepository.save(new Product("iPhone 15", "Mô tả", appleBrand, phoneCategory, ProductStatus.DRAFT));
        ProductVariant variant = productVariantRepository.save(new ProductVariant(
                product,
                "IP15-DELETE-TEST",
                "Đen",
                "128GB",
                BigDecimal.valueOf(20000000),
                BigDecimal.valueOf(22000000),
                10,
                VariantStatus.ACTIVE
        ));

        mockMvc.perform(delete("/api/v1/admin/products/" + product.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Xoá sản phẩm thành công"));

        // Entity vẫn còn trong DB (soft delete)
        Product dbProduct = productRepository.findById(product.getId()).orElseThrow();
        assertThat(dbProduct.isDeleted()).isTrue();
        assertThat(dbProduct.getDeletedAt()).isNotNull();

        ProductVariant dbVariant = productVariantRepository.findById(variant.getId()).orElseThrow();
        assertThat(dbVariant.isDeleted()).isTrue();
        assertThat(dbVariant.getDeletedAt()).isNotNull();

        // Không còn hiển thị trong danh sách sản phẩm của admin
        mockMvc.perform(get("/api/v1/admin/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));

        // Không thể lấy chi tiết qua API thông thường
        mockMvc.perform(get("/api/v1/admin/products/" + product.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Chặn xoá sản phẩm khi sản phẩm/biến thể đã phát sinh đơn hàng (400 Bad Request)")
    void deleteProduct_havingOrders_throwsBadRequest() throws Exception {
        Product product = productRepository.save(new Product("iPhone 15", null, appleBrand, phoneCategory, ProductStatus.DRAFT));
        ProductVariant variant = productVariantRepository.save(new ProductVariant(
                product,
                "IP15-ORDERED",
                "Đen",
                "128GB",
                BigDecimal.valueOf(20000000),
                BigDecimal.valueOf(22000000),
                10,
                VariantStatus.ACTIVE
        ));

        // Mock rằng variant này đã phát sinh đơn hàng
        productServiceImpl.setOrderChecker(id -> id.equals(variant.getId()));

        mockMvc.perform(delete("/api/v1/admin/products/" + product.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Không thể xoá sản phẩm đã phát sinh đơn hàng, vui lòng chuyển trạng thái sang ngừng bán"));

        Product dbProduct = productRepository.findById(product.getId()).orElseThrow();
        assertThat(dbProduct.isDeleted()).isFalse();
    }

    @Test
    @DisplayName("Chặn xoá sản phẩm khi sản phẩm không tồn tại (404 Not Found)")
    void deleteProduct_notFound_throwsNotFound() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/products/999999")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Không tìm thấy sản phẩm với ID: 999999"));
    }

    @Test
    @DisplayName("Customer truy cập API xoá sản phẩm bị cấm (403 Forbidden)")
    void deleteProduct_asCustomer_returnsForbidden() throws Exception {
        Product product = productRepository.save(new Product("iPhone 15", null, appleBrand, phoneCategory, ProductStatus.DRAFT));

        mockMvc.perform(delete("/api/v1/admin/products/" + product.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Admin lấy danh sách tất cả sản phẩm thành công")
    void getAllProducts_asAdmin_returnsList() throws Exception {
        productRepository.save(new Product("SP 1", null, appleBrand, phoneCategory, ProductStatus.DRAFT));
        productRepository.save(new Product("SP 2", null, samsungBrand, phoneCategory, ProductStatus.DRAFT));

        mockMvc.perform(get("/api/v1/admin/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)));
    }

    @Test
    @DisplayName("Admin lấy chi tiết một sản phẩm theo ID thành công")
    void getProductById_asAdmin_success() throws Exception {
        Product saved = productRepository.save(new Product("iPhone 16", "Mô tả", appleBrand, phoneCategory, ProductStatus.DRAFT));

        mockMvc.perform(get("/api/v1/admin/products/" + saved.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(saved.getId()))
                .andExpect(jsonPath("$.data.name").value("iPhone 16"));
    }

    @Test
    @DisplayName("Customer truy cập API quản trị sản phẩm bị cấm (403 Forbidden)")
    void productApi_asCustomer_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Khách vãng lai truy cập API quản trị sản phẩm bị từ chối (401 Unauthorized)")
    void productApi_unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/admin/products"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Chặn xoá thương hiệu khi đang có sản phẩm liên kết (400 Bad Request)")
    void deleteBrand_havingProducts_throwsBadRequest() throws Exception {
        productRepository.save(new Product("iPhone", null, appleBrand, phoneCategory, ProductStatus.DRAFT));

        mockMvc.perform(delete("/api/v1/admin/brands/" + appleBrand.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Không thể xoá thương hiệu đang gắn với sản phẩm"));
    }

    @Test
    @DisplayName("Chặn xoá danh mục khi đang có sản phẩm liên kết (400 Bad Request)")
    void deleteCategory_havingProducts_throwsBadRequest() throws Exception {
        productRepository.save(new Product("iPhone", null, appleBrand, phoneCategory, ProductStatus.DRAFT));

        mockMvc.perform(delete("/api/v1/admin/categories/" + phoneCategory.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Không thể xoá danh mục đang có sản phẩm gắn với nó"));
    }
}

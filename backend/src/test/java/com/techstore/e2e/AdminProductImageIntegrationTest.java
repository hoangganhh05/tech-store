package com.techstore.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techstore.dto.request.ProductImageUpdateRequest;
import com.techstore.entity.Brand;
import com.techstore.entity.Category;
import com.techstore.entity.Product;
import com.techstore.entity.ProductImage;
import com.techstore.entity.ProductVariant;
import com.techstore.entity.Role;
import com.techstore.entity.User;
import com.techstore.enums.ErrorCode;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class AdminProductImageIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
    private ProductVariant testVariant;

    @BeforeEach
    void setUp() {
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

        Brand apple = brandRepository.save(new Brand("Apple", "https://example.com/apple.png", "Apple Inc"));
        Category phone = categoryRepository.save(new Category("Điện thoại", "Smartphone", null, null));

        testProduct = productRepository.save(new Product("iPhone 16 Pro", "Flagship", apple, phone, ProductStatus.DRAFT));
        secondProduct = productRepository.save(new Product("iPhone 16", "Standard", apple, phone, ProductStatus.DRAFT));

        testVariant = new ProductVariant(testProduct, "IP16P-BLK-128", "Titan Đen", "128GB", new BigDecimal("28990000"), new BigDecimal("30990000"), 10, VariantStatus.ACTIVE);
        testVariant = productVariantRepository.save(testVariant);
    }

    @AfterEach
    void tearDown() {
        productImageRepository.deleteAll();
        productVariantRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        brandRepository.deleteAll();
    }

    @Test
    @DisplayName("Admin upload ảnh đầu tiên -> tự động thành ảnh đại diện (isPrimary = true)")
    void uploadImage_firstImage_becomesPrimary() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "iphone-front.png",
                "image/png",
                "mock-png-content".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/admin/products/{id}/images", testProduct.getId())
                        .file(file)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isPrimary").value(true))
                .andExpect(jsonPath("$.data.productId").value(testProduct.getId()))
                .andExpect(jsonPath("$.data.imageUrl").isNotEmpty());

        assertThat(productImageRepository.countByProductId(testProduct.getId())).isEqualTo(1);
    }

    @Test
    @DisplayName("Admin upload ảnh thứ hai với isPrimary = true -> demote ảnh cũ")
    void uploadImage_withPrimaryTrue_demotesPrevious() throws Exception {
        MockMultipartFile file1 = new MockMultipartFile("file", "img1.jpg", "image/jpeg", "image1".getBytes());
        MockMultipartFile file2 = new MockMultipartFile("file", "img2.jpg", "image/jpeg", "image2".getBytes());

        mockMvc.perform(multipart("/api/v1/admin/products/{id}/images", testProduct.getId())
                        .file(file1)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.isPrimary").value(true));

        mockMvc.perform(multipart("/api/v1/admin/products/{id}/images", testProduct.getId())
                        .file(file2)
                        .param("isPrimary", "true")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.isPrimary").value(true));

        List<ProductImage> images = productImageRepository.findByProductIdOrderByIsPrimaryDescDisplayOrderAscIdAsc(testProduct.getId());
        assertThat(images).hasSize(2);
        assertThat(images.get(0).getIsPrimary()).isTrue();
        assertThat(images.get(1).getIsPrimary()).isFalse();
    }

    @Test
    @DisplayName("Admin upload ảnh gắn theo biến thể thành công")
    void uploadImage_withVariant_success() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "black.webp", "image/webp", "webp-data".getBytes());

        mockMvc.perform(multipart("/api/v1/admin/products/{id}/images", testProduct.getId())
                        .file(file)
                        .param("variantId", testVariant.getId().toString())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.variantId").value(testVariant.getId()))
                .andExpect(jsonPath("$.data.variantSku").value("IP16P-BLK-128"))
                .andExpect(jsonPath("$.data.variantColor").value("Titan Đen"));
    }

    @Test
    @DisplayName("Admin upload ảnh với variant không thuộc sản phẩm -> lỗi 404")
    void uploadImage_variantFromAnotherProduct_returns404() throws Exception {
        ProductVariant variant2 = new ProductVariant(secondProduct, "IP16-WHT-128", "Trắng", "128GB", new BigDecimal("22000000"), null, 5, VariantStatus.ACTIVE);
        variant2 = productVariantRepository.save(variant2);

        MockMultipartFile file = new MockMultipartFile("file", "wrong.jpg", "image/jpeg", "data".getBytes());

        mockMvc.perform(multipart("/api/v1/admin/products/{id}/images", testProduct.getId())
                        .file(file)
                        .param("variantId", variant2.getId().toString())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.PRODUCT_VARIANT_NOT_FOUND.name()));
    }

    @Test
    @DisplayName("Upload định dạng file không hợp lệ (e.g. PDF/TXT) -> lỗi 400 INVALID_IMAGE_FORMAT")
    void uploadImage_invalidExtension_returns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", "pdf-data".getBytes());

        mockMvc.perform(multipart("/api/v1/admin/products/{id}/images", testProduct.getId())
                        .file(file)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_IMAGE_FORMAT.name()));
    }

    @Test
    @DisplayName("Upload file vượt quá 5MB -> lỗi 400 IMAGE_SIZE_EXCEEDED")
    void uploadImage_sizeExceeded_returns400() throws Exception {
        byte[] largeBytes = new byte[5 * 1024 * 1024 + 10];
        MockMultipartFile file = new MockMultipartFile("file", "large.png", "image/png", largeBytes);

        mockMvc.perform(multipart("/api/v1/admin/products/{id}/images", testProduct.getId())
                        .file(file)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.IMAGE_SIZE_EXCEEDED.name()));
    }

    @Test
    @DisplayName("Admin lấy danh sách ảnh của sản phẩm theo thứ tự primary trước")
    void getImages_returnsOrderedList() throws Exception {
        ProductImage img1 = productImageRepository.save(new ProductImage(testProduct, null, "/uploads/products/1.jpg", false, 0));
        ProductImage img2 = productImageRepository.save(new ProductImage(testProduct, null, "/uploads/products/2.jpg", true, 1));

        mockMvc.perform(get("/api/v1/admin/products/{id}/images", testProduct.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].id").value(img2.getId()))
                .andExpect(jsonPath("$.data[0].isPrimary").value(true));
    }

    @Test
    @DisplayName("Admin đổi ảnh đại diện (set primary)")
    void setPrimaryImage_success() throws Exception {
        ProductImage img1 = productImageRepository.save(new ProductImage(testProduct, null, "/uploads/products/1.jpg", true, 0));
        ProductImage img2 = productImageRepository.save(new ProductImage(testProduct, null, "/uploads/products/2.jpg", false, 1));

        mockMvc.perform(put("/api/v1/admin/products/{id}/images/{imageId}/primary", testProduct.getId(), img2.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(img2.getId()))
                .andExpect(jsonPath("$.data.isPrimary").value(true));

        ProductImage updated1 = productImageRepository.findById(img1.getId()).orElseThrow();
        ProductImage updated2 = productImageRepository.findById(img2.getId()).orElseThrow();
        assertThat(updated1.getIsPrimary()).isFalse();
        assertThat(updated2.getIsPrimary()).isTrue();
    }

    @Test
    @DisplayName("Admin cập nhật gắn biến thể cho ảnh")
    void updateImage_assignVariant_success() throws Exception {
        ProductImage img = productImageRepository.save(new ProductImage(testProduct, null, "/uploads/products/1.jpg", true, 0));
        ProductImageUpdateRequest request = new ProductImageUpdateRequest(testVariant.getId(), 5);

        mockMvc.perform(put("/api/v1/admin/products/{id}/images/{imageId}", testProduct.getId(), img.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.variantId").value(testVariant.getId()))
                .andExpect(jsonPath("$.data.displayOrder").value(5));
    }

    @Test
    @DisplayName("Admin xoá ảnh đại diện -> ảnh còn lại được thăng hạng làm đại diện")
    void deleteImage_primaryDeleted_promotesRemaining() throws Exception {
        ProductImage img1 = productImageRepository.save(new ProductImage(testProduct, null, "/uploads/products/1.jpg", true, 0));
        ProductImage img2 = productImageRepository.save(new ProductImage(testProduct, null, "/uploads/products/2.jpg", false, 1));

        mockMvc.perform(delete("/api/v1/admin/products/{id}/images/{imageId}", testProduct.getId(), img1.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        assertThat(productImageRepository.existsById(img1.getId())).isFalse();
        ProductImage remaining = productImageRepository.findById(img2.getId()).orElseThrow();
        assertThat(remaining.getIsPrimary()).isTrue();
    }

    @Test
    @DisplayName("Customer truy cập API upload ảnh -> 403 Forbidden")
    void uploadImage_asCustomer_returnsForbidden() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "content".getBytes());

        mockMvc.perform(multipart("/api/v1/admin/products/{id}/images", testProduct.getId())
                        .file(file)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCode.ACCESS_DENIED.name()));
    }

    @Test
    @DisplayName("Truy cập không token -> 401 Unauthorized")
    void uploadImage_withoutToken_returnsUnauthorized() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "content".getBytes());

        mockMvc.perform(multipart("/api/v1/admin/products/{id}/images", testProduct.getId())
                        .file(file))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_ACCESS_TOKEN.name()));
    }
}

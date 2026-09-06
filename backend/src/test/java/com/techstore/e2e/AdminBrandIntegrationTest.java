package com.techstore.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techstore.dto.request.BrandRequest;
import com.techstore.entity.Brand;
import com.techstore.entity.Role;
import com.techstore.entity.User;
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
class AdminBrandIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductSpecificationRepository productSpecificationRepository;

    @Autowired
    private ProductImageRepository productImageRepository;

    @Autowired
    private ProductVariantRepository productVariantRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private BrandRepository brandRepository;

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
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
    }

    private void cleanDatabase() {
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
    @DisplayName("Admin tạo thương hiệu mới thành công")
    void createBrand_asAdmin_success() throws Exception {
        BrandRequest request = new BrandRequest("Apple", "https://example.com/apple.png", "Thương hiệu Apple");

        mockMvc.perform(post("/api/v1/admin/brands")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Apple"))
                .andExpect(jsonPath("$.data.logoUrl").value("https://example.com/apple.png"))
                .andExpect(jsonPath("$.data.description").value("Thương hiệu Apple"))
                .andExpect(jsonPath("$.data.id").isNumber());

        assertThat(brandRepository.existsByNameIgnoreCase("Apple")).isTrue();
    }

    @Test
    @DisplayName("Chặn tạo thương hiệu trùng tên (case-insensitive)")
    void createBrand_duplicateName_throwsBadRequest() throws Exception {
        brandRepository.save(new Brand("Samsung", "https://example.com/samsung.png", "Mô tả"));

        BrandRequest request = new BrandRequest("samsung", null, "Trùng tên");

        mockMvc.perform(post("/api/v1/admin/brands")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("BRAND_NAME_DUPLICATE"));
    }

    @Test
    @DisplayName("Lấy danh sách thương hiệu sắp xếp theo tên ASC")
    void getAllBrands_asAdmin_returnsOrderedList() throws Exception {
        brandRepository.save(new Brand("Xiaomi", null, null));
        brandRepository.save(new Brand("Apple", null, null));
        brandRepository.save(new Brand("Dell", null, null));

        mockMvc.perform(get("/api/v1/admin/brands")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(3)))
                .andExpect(jsonPath("$.data[0].name").value("Apple"))
                .andExpect(jsonPath("$.data[1].name").value("Dell"))
                .andExpect(jsonPath("$.data[2].name").value("Xiaomi"));
    }

    @Test
    @DisplayName("Lấy chi tiết thương hiệu theo ID thành công")
    void getBrandById_asAdmin_success() throws Exception {
        Brand brand = brandRepository.save(new Brand("Sony", "https://example.com/sony.png", "Sony Corp"));

        mockMvc.perform(get("/api/v1/admin/brands/" + brand.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Sony"))
                .andExpect(jsonPath("$.data.description").value("Sony Corp"));
    }

    @Test
    @DisplayName("Lấy chi tiết thương hiệu không tồn tại trả về 404")
    void getBrandById_notFound_throwsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/admin/brands/999999")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("BRAND_NOT_FOUND"));
    }

    @Test
    @DisplayName("Cập nhật thông tin thương hiệu thành công")
    void updateBrand_asAdmin_success() throws Exception {
        Brand brand = brandRepository.save(new Brand("Asus", null, null));

        BrandRequest updateRequest = new BrandRequest("ASUS ROG", "https://example.com/rog.png", "Chuyên gaming");

        mockMvc.perform(put("/api/v1/admin/brands/" + brand.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("ASUS ROG"))
                .andExpect(jsonPath("$.data.logoUrl").value("https://example.com/rog.png"))
                .andExpect(jsonPath("$.data.description").value("Chuyên gaming"));
    }

    @Test
    @DisplayName("Chặn cập nhật thương hiệu trùng tên với thương hiệu khác")
    void updateBrand_duplicateNameWithOther_throwsBadRequest() throws Exception {
        brandRepository.save(new Brand("Apple", null, null));
        Brand brand = brandRepository.save(new Brand("Asus", null, null));

        BrandRequest updateRequest = new BrandRequest("apple", null, null);

        mockMvc.perform(put("/api/v1/admin/brands/" + brand.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BRAND_NAME_DUPLICATE"));
    }

    @Test
    @DisplayName("Xoá thương hiệu thành công")
    void deleteBrand_asAdmin_success() throws Exception {
        Brand brand = brandRepository.save(new Brand("Lenovo", null, null));

        mockMvc.perform(delete("/api/v1/admin/brands/" + brand.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        assertThat(brandRepository.findById(brand.getId())).isEmpty();
    }

    @Test
    @DisplayName("Truy cập API quản trị thương hiệu khi không có token trả về 401")
    void accessBrandApi_withoutToken_throwsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/admin/brands"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_ACCESS_TOKEN"));
    }

    @Test
    @DisplayName("Truy cập API quản trị thương hiệu bằng tài khoản khách hàng trả về 403")
    void accessBrandApi_asCustomer_throwsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/brands")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }
}

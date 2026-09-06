package com.techstore.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techstore.dto.request.CategoryDisplayRequest;
import com.techstore.dto.request.CategoryRequest;
import com.techstore.entity.Category;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class AdminCategoryIntegrationTest {

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
    @DisplayName("Admin tạo danh mục gốc thành công")
    void createRootCategory_asAdmin_success() throws Exception {
        CategoryRequest request = new CategoryRequest(
                "Điện thoại",
                "Danh mục các loại điện thoại thông minh",
                null,
                "https://example.com/phone.png"
        );

        mockMvc.perform(post("/api/v1/admin/categories")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Điện thoại"))
                .andExpect(jsonPath("$.data.description").value("Danh mục các loại điện thoại thông minh"))
                .andExpect(jsonPath("$.data.parentId").doesNotExist())
                .andExpect(jsonPath("$.data.imageUrl").value("https://example.com/phone.png"));
    }

    @Test
    @DisplayName("Admin tạo danh mục con với danh mục cha thành công")
    void createChildCategory_asAdmin_success() throws Exception {
        Category parent = categoryRepository.save(new Category("Điện thoại", "Gốc", null, null));

        CategoryRequest request = new CategoryRequest(
                "iPhone",
                "Điện thoại Apple",
                parent.getId(),
                "https://example.com/iphone.png"
        );

        mockMvc.perform(post("/api/v1/admin/categories")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("iPhone"))
                .andExpect(jsonPath("$.data.parentId").value(parent.getId()))
                .andExpect(jsonPath("$.data.parentName").value("Điện thoại"));
    }

    @Test
    @DisplayName("Chặn tạo danh mục trùng tên trong cùng một cấp (400 Bad Request)")
    void createCategory_duplicateNameInSameLevel_throwsBadRequest() throws Exception {
        categoryRepository.save(new Category("Điện thoại", "Gốc", null, null));

        CategoryRequest duplicate = new CategoryRequest("Điện thoại", "Trùng", null, null);

        mockMvc.perform(post("/api/v1/admin/categories")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicate)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Tên danh mục đã tồn tại trong cùng một cấp"));
    }

    @Test
    @DisplayName("Cho phép tạo danh mục trùng tên nếu ở khác cấp")
    void createCategory_sameNameDifferentLevel_success() throws Exception {
        Category parent = categoryRepository.save(new Category("Apple", "Hãng Apple", null, null));

        CategoryRequest childWithSameName = new CategoryRequest("Apple", "Phụ kiện Apple", parent.getId(), null);

        mockMvc.perform(post("/api/v1/admin/categories")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(childWithSameName)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Apple"))
                .andExpect(jsonPath("$.data.parentId").value(parent.getId()));
    }

    @Test
    @DisplayName("Lấy danh sách cây danh mục phân cấp chính xác")
    void getCategoryTree_asAdmin_returnsTreeStructure() throws Exception {
        Category root1 = categoryRepository.save(new Category("Điện thoại", null, null, null));
        Category root2 = categoryRepository.save(new Category("Laptop", null, null, null));
        Category child1 = categoryRepository.save(new Category("iPhone", null, root1, null));
        categoryRepository.save(new Category("MacBook", null, root2, null));
        categoryRepository.save(new Category("iPhone 16", null, child1, null));

        mockMvc.perform(get("/api/v1/admin/categories/tree")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[?(@.name == 'Điện thoại')].children[0].name").value("iPhone"))
                .andExpect(jsonPath("$.data[?(@.name == 'Điện thoại')].children[0].children[0].name").value("iPhone 16"))
                .andExpect(jsonPath("$.data[?(@.name == 'Laptop')].children[0].name").value("MacBook"));
    }

    @Test
    @DisplayName("Cập nhật danh mục thành công")
    void updateCategory_asAdmin_success() throws Exception {
        Category category = categoryRepository.save(new Category("Cũ", "Mô tả cũ", null, null));

        CategoryRequest updateRequest = new CategoryRequest("Mới", "Mô tả mới", null, "https://example.com/new.png");

        mockMvc.perform(put("/api/v1/admin/categories/" + category.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Mới"))
                .andExpect(jsonPath("$.data.description").value("Mô tả mới"))
                .andExpect(jsonPath("$.data.imageUrl").value("https://example.com/new.png"));
    }

    @Test
    @DisplayName("Chặn xoá danh mục khi đang có danh mục con (400 Bad Request)")
    void deleteCategory_havingChildren_throwsBadRequest() throws Exception {
        Category parent = categoryRepository.save(new Category("Cha", null, null, null));
        categoryRepository.save(new Category("Con", null, parent, null));

        mockMvc.perform(delete("/api/v1/admin/categories/" + parent.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Không thể xoá danh mục đang có danh mục con gắn với nó"));

        assertThat(categoryRepository.existsById(parent.getId())).isTrue();
    }

    @Test
    @DisplayName("Xoá danh mục lá thành công")
    void deleteCategory_leaf_success() throws Exception {
        Category leaf = categoryRepository.save(new Category("Lá", null, null, null));

        mockMvc.perform(delete("/api/v1/admin/categories/" + leaf.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Xoá danh mục thành công"));

        assertThat(categoryRepository.existsById(leaf.getId())).isFalse();
    }

    @Test
    @DisplayName("Customer truy cập API quản lý danh mục bị cấm (403 Forbidden)")
    void categoryApi_asCustomer_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/categories")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Khách vãng lai chưa đăng nhập truy cập API bị từ chối (401 Unauthorized)")
    void categoryApi_unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/admin/categories"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Cập nhật thứ tự hiển thị và trạng thái active thành công (PATCH display)")
    void updateCategoryDisplay_success() throws Exception {
        Category category = categoryRepository.save(new Category("Danh mục test hiển thị", null, null, null));
        CategoryDisplayRequest request = new CategoryDisplayRequest(5, false);

        mockMvc.perform(patch("/api/v1/admin/categories/" + category.getId() + "/display")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(category.getId()))
                .andExpect(jsonPath("$.data.displayOrder").value(5))
                .andExpect(jsonPath("$.data.isActive").value(false));

        Category updated = categoryRepository.findById(category.getId()).orElseThrow();
        assertThat(updated.getDisplayOrder()).isEqualTo(5);
        assertThat(updated.getIsActive()).isFalse();
    }

    @Test
    @DisplayName("Cập nhật thứ tự hiển thị thất bại khi validate lỗi (displayOrder âm hoặc null)")
    void updateCategoryDisplay_validationError() throws Exception {
        Category category = categoryRepository.save(new Category("Danh mục lỗi validate", null, null, null));
        CategoryDisplayRequest request = new CategoryDisplayRequest(-1, true);

        mockMvc.perform(patch("/api/v1/admin/categories/" + category.getId() + "/display")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("API Storefront public chỉ trả về danh mục active theo thứ tự displayOrder")
    void getPublicCategoryTree_onlyReturnsActiveCategoriesInOrder() throws Exception {
        // Root 1: active, displayOrder = 20
        Category rootA = new Category("Danh mục A", null, null, null);
        rootA.updateDisplay(20, true);
        categoryRepository.save(rootA);

        // Root 2: inactive, displayOrder = 5
        Category rootB = new Category("Danh mục B ẩn", null, null, null);
        rootB.updateDisplay(5, false);
        categoryRepository.save(rootB);

        // Root 3: active, displayOrder = 10 (có con: 1 active, 1 inactive)
        Category rootC = new Category("Danh mục C", null, null, null);
        rootC.updateDisplay(10, true);
        rootC = categoryRepository.save(rootC);

        Category childActive = new Category("Con active", null, rootC, null);
        childActive.updateDisplay(1, true);
        categoryRepository.save(childActive);

        Category childInactive = new Category("Con inactive", null, rootC, null);
        childInactive.updateDisplay(2, false);
        categoryRepository.save(childInactive);

        // Call public storefront API (no auth header needed)
        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2))) // B is excluded
                .andExpect(jsonPath("$.data[0].name").value("Danh mục C")) // displayOrder 10
                .andExpect(jsonPath("$.data[0].children", hasSize(1))) // only childActive
                .andExpect(jsonPath("$.data[0].children[0].name").value("Con active"))
                .andExpect(jsonPath("$.data[1].name").value("Danh mục A")); // displayOrder 20
    }

    @Test
    @DisplayName("Customer cập nhật thứ tự hiển thị bị cấm (403 Forbidden)")
    void updateCategoryDisplay_asCustomer_forbidden() throws Exception {
        Category category = categoryRepository.save(new Category("Danh mục cấm sửa", null, null, null));
        CategoryDisplayRequest request = new CategoryDisplayRequest(1, true);

        mockMvc.perform(patch("/api/v1/admin/categories/" + category.getId() + "/display")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}

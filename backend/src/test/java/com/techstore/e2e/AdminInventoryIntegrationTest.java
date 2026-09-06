package com.techstore.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.techstore.repository.BrandRepository;
import com.techstore.repository.CategoryRepository;
import com.techstore.repository.InventoryRepository;
import com.techstore.repository.InventoryTransactionRepository;
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
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class AdminInventoryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InventoryTransactionRepository inventoryTransactionRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

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

    private Category phoneCategory;
    private Category laptopCategory;
    private Brand appleBrand;
    private Product iphone;
    private Product macbook;
    private ProductVariant iphone128;
    private ProductVariant iphone256;
    private ProductVariant macbookBase;

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

        phoneCategory = categoryRepository.save(new Category("Điện thoại", "Điện thoại thông minh", null, null));
        laptopCategory = categoryRepository.save(new Category("Laptop", "Máy tính xách tay", null, null));
        appleBrand = brandRepository.save(new Brand("Apple", "https://example.com/apple.png", "Apple Inc"));

        iphone = productRepository.save(new Product("iPhone 16 Pro", "Flagship Phone", appleBrand, phoneCategory, ProductStatus.ACTIVE));
        macbook = productRepository.save(new Product("MacBook Pro M3", "Pro Laptop", appleBrand, laptopCategory, ProductStatus.ACTIVE));

        // Variant 1: In stock (on hand: 20, reserved: 2 -> available: 18, threshold: 5) -> IN_STOCK
        iphone128 = productVariantRepository.save(new ProductVariant(
                iphone, "IP16P-128-BLK", "Đen", "128GB",
                new BigDecimal("28000000.00"), new BigDecimal("30000000.00"), 20, VariantStatus.ACTIVE
        ));
        inventoryRepository.save(new Inventory(iphone128, 20, 2, 5));

        // Variant 2: Low stock (on hand: 4, reserved: 1 -> available: 3, threshold: 5) -> LOW_STOCK
        iphone256 = productVariantRepository.save(new ProductVariant(
                iphone, "IP16P-256-WHT", "Trắng", "256GB",
                new BigDecimal("31000000.00"), null, 4, VariantStatus.ACTIVE
        ));
        inventoryRepository.save(new Inventory(iphone256, 4, 1, 5));

        // Variant 3: Out of stock (on hand: 1, reserved: 1 -> available: 0, threshold: 5) -> OUT_OF_STOCK
        macbookBase = productVariantRepository.save(new ProductVariant(
                macbook, "MBP-M3-SILVER", "Bạc", "512GB",
                new BigDecimal("45000000.00"), null, 1, VariantStatus.ACTIVE
        ));
        inventoryRepository.save(new Inventory(macbookBase, 1, 1, 5));
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
    @DisplayName("US-04.1 T-04.1.2: Truy cập API tồn kho khi chưa đăng nhập trả về 401 Unauthorized")
    void getInventories_unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/admin/inventory"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("US-04.1 T-04.1.2: Khách hàng CUSTOMER truy cập API tồn kho trả về 403 Forbidden")
    void getInventories_asCustomer_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/inventory")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("US-04.1 T-04.1.1: Admin xem danh sách tồn kho mặc định phân trang thành công")
    void getInventories_asAdmin_returnsPaginatedList() throws Exception {
        mockMvc.perform(get("/api/v1/admin/inventory")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.totalElements", is(3)))
                .andExpect(jsonPath("$.data.items", hasSize(3)));
    }

    @Test
    @DisplayName("US-04.1 T-04.1.1: Tìm kiếm theo tên sản phẩm hoặc SKU")
    void getInventories_filterBySearch_matchesProductNameOrSku() throws Exception {
        // Search by Product name
        mockMvc.perform(get("/api/v1/admin/inventory")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("search", "MacBook"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements", is(1)))
                .andExpect(jsonPath("$.data.items[0].sku", is("MBP-M3-SILVER")));

        // Search by SKU
        mockMvc.perform(get("/api/v1/admin/inventory")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("search", "128-BLK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements", is(1)))
                .andExpect(jsonPath("$.data.items[0].sku", is("IP16P-128-BLK")));
    }

    @Test
    @DisplayName("US-04.1 T-04.1.1: Lọc tồn kho theo danh mục")
    void getInventories_filterByCategory_returnsMatchingOnly() throws Exception {
        mockMvc.perform(get("/api/v1/admin/inventory")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("categoryId", laptopCategory.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements", is(1)))
                .andExpect(jsonPath("$.data.items[0].productName", is("MacBook Pro M3")));
    }

    @Test
    @DisplayName("US-04.1 T-04.1.1: Lọc theo trạng thái tồn kho IN_STOCK, LOW_STOCK, OUT_OF_STOCK")
    void getInventories_filterByStockStatus() throws Exception {
        // IN_STOCK
        mockMvc.perform(get("/api/v1/admin/inventory")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("stockStatus", "IN_STOCK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements", is(1)))
                .andExpect(jsonPath("$.data.items[0].sku", is("IP16P-128-BLK")))
                .andExpect(jsonPath("$.data.items[0].stockStatus", is("IN_STOCK")));

        // LOW_STOCK
        mockMvc.perform(get("/api/v1/admin/inventory")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("stockStatus", "LOW_STOCK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements", is(1)))
                .andExpect(jsonPath("$.data.items[0].sku", is("IP16P-256-WHT")))
                .andExpect(jsonPath("$.data.items[0].stockStatus", is("LOW_STOCK")));

        // OUT_OF_STOCK
        mockMvc.perform(get("/api/v1/admin/inventory")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("stockStatus", "OUT_OF_STOCK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements", is(1)))
                .andExpect(jsonPath("$.data.items[0].sku", is("MBP-M3-SILVER")))
                .andExpect(jsonPath("$.data.items[0].stockStatus", is("OUT_OF_STOCK")));
    }

    @Test
    @DisplayName("US-04.1 T-04.1.1: Số lượng khả dụng tính đúng theo thời gian thực (onHand - reserved)")
    void getInventories_calculatesAvailableQuantityCorrectly() throws Exception {
        mockMvc.perform(get("/api/v1/admin/inventory")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("search", "IP16P-128-BLK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].quantityOnHand", is(20)))
                .andExpect(jsonPath("$.data.items[0].quantityReserved", is(2)))
                .andExpect(jsonPath("$.data.items[0].availableQuantity", is(18)));
    }

    @Test
    @DisplayName("US-04.1 T-04.1.1: Thống kê tổng quan tồn kho (summary)")
    void getSummary_returnsCorrectCounts() throws Exception {
        mockMvc.perform(get("/api/v1/admin/inventory/summary")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.totalVariants", is(3)))
                .andExpect(jsonPath("$.data.inStockCount", is(1)))
                .andExpect(jsonPath("$.data.lowStockCount", is(1)))
                .andExpect(jsonPath("$.data.outOfStockCount", is(1)));
    }

    @Test
    @DisplayName("US-04.1 T-04.1.1: Xem chi tiết tồn kho của một biến thể thành công")
    void getByVariantId_success() throws Exception {
        mockMvc.perform(get("/api/v1/admin/inventory/variants/" + iphone128.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.variantId", is(iphone128.getId().intValue())))
                .andExpect(jsonPath("$.data.sku", is("IP16P-128-BLK")))
                .andExpect(jsonPath("$.data.availableQuantity", is(18)))
                .andExpect(jsonPath("$.data.stockStatus", is("IN_STOCK")));
    }

    @Test
    @DisplayName("US-04.1 T-04.1.2: Xem chi tiết tồn kho với variantId không tồn tại trả về 404")
    void getByVariantId_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/admin/inventory/variants/999999")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.code", is("INVENTORY_NOT_FOUND")));
    }
}

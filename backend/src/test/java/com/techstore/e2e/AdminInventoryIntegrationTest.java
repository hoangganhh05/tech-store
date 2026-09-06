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

import com.techstore.dto.request.InventoryAdjustmentRequest;
import com.techstore.dto.request.InventoryImportRequest;
import com.techstore.dto.request.OrderInventoryDeductionRequest;
import com.techstore.dto.request.OrderInventoryRestoreRequest;
import com.techstore.dto.request.OrderItemStockRequest;
import com.techstore.enums.InventoryTransactionType;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

    @Test
    @DisplayName("US-04.2 T-04.2.1: Nhập kho tăng số lượng tồn và tạo lịch sử giao dịch thành công")
    void importInventory_success() throws Exception {
        InventoryImportRequest request = new InventoryImportRequest(
                iphone128.getId(),
                15,
                "Nhập lô hàng Apple mới từ nhà phân phối",
                "PURCHASE_ORDER",
                1001L
        );

        mockMvc.perform(post("/api/v1/admin/inventory/import")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.variantId", is(iphone128.getId().intValue())))
                .andExpect(jsonPath("$.data.quantityOnHand", is(35))) // 20 + 15
                .andExpect(jsonPath("$.data.quantityReserved", is(2)))
                .andExpect(jsonPath("$.data.availableQuantity", is(33))) // 35 - 2
                .andExpect(jsonPath("$.data.stockStatus", is("IN_STOCK")));

        // Verify database state for variant and inventory transaction
        ProductVariant updatedVariant = productVariantRepository.findById(iphone128.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(35, updatedVariant.getStockQuantity());

        Inventory inv = inventoryRepository.findByVariantId(iphone128.getId()).orElseThrow();
        var transactions = inventoryTransactionRepository.findByInventoryIdOrderByCreatedAtDesc(inv.getId());
        org.junit.jupiter.api.Assertions.assertFalse(transactions.isEmpty());
        var tx = transactions.get(0);
        org.junit.jupiter.api.Assertions.assertEquals(15, tx.getQuantityChange());
        org.junit.jupiter.api.Assertions.assertEquals("Nhập lô hàng Apple mới từ nhà phân phối", tx.getNote());
        org.junit.jupiter.api.Assertions.assertNotNull(tx.getCreatedBy());
        org.junit.jupiter.api.Assertions.assertNotNull(tx.getCreatedBy().getId());
    }

    @Test
    @DisplayName("US-04.2 T-04.2.2: Nhập kho với số lượng nhỏ hơn hoặc bằng 0 trả về 400")
    void importInventory_invalidQuantity_returns400() throws Exception {
        InventoryImportRequest request = new InventoryImportRequest(
                iphone128.getId(),
                0,
                "Nhập 0 cái",
                null,
                null
        );

        mockMvc.perform(post("/api/v1/admin/inventory/import")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("US-04.2 T-04.2.2: Nhập kho với variantId không tồn tại trả về 404")
    void importInventory_variantNotFound_returns404() throws Exception {
        InventoryImportRequest request = new InventoryImportRequest(
                999999L,
                10,
                "Nhập variant ảo",
                null,
                null
        );

        mockMvc.perform(post("/api/v1/admin/inventory/import")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.code", is("PRODUCT_VARIANT_NOT_FOUND")));
    }

    @Test
    @DisplayName("US-04.2 T-04.2.2: Phân quyền - Người dùng không phải Admin bị từ chối 403")
    void importInventory_forbiddenForCustomer_returns403() throws Exception {
        InventoryImportRequest request = new InventoryImportRequest(
                iphone128.getId(),
                10,
                "Customer thử hack",
                null,
                null
        );

        mockMvc.perform(post("/api/v1/admin/inventory/import")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.code", is("ACCESS_DENIED")));
    }

    @Test
    @DisplayName("US-04.2 T-04.2.1: Lấy danh sách lịch sử giao dịch kho phân trang")
    void getTransactions_success() throws Exception {
        // First perform an import
        InventoryImportRequest importReq = new InventoryImportRequest(
                iphone128.getId(),
                5,
                "Lô thử nghiệm",
                "MANUAL_IMPORT",
                null
        );

        mockMvc.perform(post("/api/v1/admin/inventory/import")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(importReq)))
                .andExpect(status().isOk());

        // Then query transactions
        mockMvc.perform(get("/api/v1/admin/inventory/transactions")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("variantId", iphone128.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.items[0].sku", is("IP16P-128-BLK")))
                .andExpect(jsonPath("$.data.items[0].quantityChange", is(5)))
                .andExpect(jsonPath("$.data.items[0].note", is("Lô thử nghiệm")))
                .andExpect(jsonPath("$.data.items[0].createdByName", is("Admin User")));
    }

    @Test
    @DisplayName("US-04.3 T-04.3.1: Điều chỉnh tăng tồn kho thủ công thành công (ADJUSTMENT > 0)")
    void adjustInventory_increase_success() throws Exception {
        InventoryAdjustmentRequest request = new InventoryAdjustmentRequest(
                iphone128.getId(),
                8,
                "Kiểm kê phát hiện thừa 8 máy",
                null,
                null
        );

        mockMvc.perform(post("/api/v1/admin/inventory/adjust")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.quantityOnHand", is(28)))
                .andExpect(jsonPath("$.data.availableQuantity", is(26)))
                .andExpect(jsonPath("$.data.stockStatus", is("IN_STOCK")));

        mockMvc.perform(get("/api/v1/admin/inventory/transactions")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("variantId", iphone128.getId().toString())
                        .param("type", "ADJUSTMENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.items[0].transactionType", is("ADJUSTMENT")))
                .andExpect(jsonPath("$.data.items[0].quantityChange", is(8)))
                .andExpect(jsonPath("$.data.items[0].note", is("Kiểm kê phát hiện thừa 8 máy")));
    }

    @Test
    @DisplayName("US-04.3 T-04.3.1: Điều chỉnh giảm tồn kho thủ công thành công (ADJUSTMENT < 0)")
    void adjustInventory_decrease_success() throws Exception {
        InventoryAdjustmentRequest request = new InventoryAdjustmentRequest(
                iphone128.getId(),
                -5,
                "Hàng vỡ màn hình trong quá trình kiểm kê",
                null,
                null
        );

        mockMvc.perform(post("/api/v1/admin/inventory/adjust")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.quantityOnHand", is(15)))
                .andExpect(jsonPath("$.data.availableQuantity", is(13)));

        mockMvc.perform(get("/api/v1/admin/inventory/transactions")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("variantId", iphone128.getId().toString())
                        .param("type", "ADJUSTMENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.items[0].transactionType", is("ADJUSTMENT")))
                .andExpect(jsonPath("$.data.items[0].quantityChange", is(-5)))
                .andExpect(jsonPath("$.data.items[0].note", is("Hàng vỡ màn hình trong quá trình kiểm kê")));
    }

    @Test
    @DisplayName("US-04.3 T-04.3.1: Điều chỉnh làm tồn kho âm bị từ chối")
    void adjustInventory_negativeStock_fails() throws Exception {
        InventoryAdjustmentRequest request = new InventoryAdjustmentRequest(
                iphone128.getId(),
                -25,
                "Thất thoát toàn bộ kho",
                null,
                null
        );

        mockMvc.perform(post("/api/v1/admin/inventory/adjust")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.code", is("INVALID_STOCK_QUANTITY")));
    }

    @Test
    @DisplayName("US-04.3 T-04.3.1: Điều chỉnh làm tồn kho nhỏ hơn số lượng đang giữ bị từ chối")
    void adjustInventory_belowReserved_fails() throws Exception {
        Inventory inv = inventoryRepository.findByVariantId(iphone128.getId()).orElseThrow();
        inv.setQuantityReserved(15);
        inventoryRepository.save(inv);

        InventoryAdjustmentRequest request = new InventoryAdjustmentRequest(
                iphone128.getId(),
                -10,
                "Giảm tồn",
                null,
                null
        );

        mockMvc.perform(post("/api/v1/admin/inventory/adjust")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.code", is("INVALID_STOCK_QUANTITY")));
    }

    @Test
    @DisplayName("US-04.3 T-04.3.2: Điều chỉnh với quantityChange = 0 bị từ chối")
    void adjustInventory_zeroChange_fails() throws Exception {
        InventoryAdjustmentRequest request = new InventoryAdjustmentRequest(
                iphone128.getId(),
                0,
                "Không thay đổi gì",
                null,
                null
        );

        mockMvc.perform(post("/api/v1/admin/inventory/adjust")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.code", is("INVALID_STOCK_QUANTITY")));
    }

    @Test
    @DisplayName("US-04.3 T-04.3.2: Lý do điều chỉnh bị trống bị từ chối validation")
    void adjustInventory_blankReason_fails() throws Exception {
        InventoryAdjustmentRequest request = new InventoryAdjustmentRequest(
                iphone128.getId(),
                2,
                "   ",
                null,
                null
        );

        mockMvc.perform(post("/api/v1/admin/inventory/adjust")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("US-04.3 T-04.3.2: Khách hàng gọi API điều chỉnh bị 403 Forbidden")
    void adjustInventory_forbiddenForCustomer_returns403() throws Exception {
        InventoryAdjustmentRequest request = new InventoryAdjustmentRequest(
                iphone128.getId(),
                5,
                "Khách hàng cố tình can thiệp kho",
                null,
                null
        );

        mockMvc.perform(post("/api/v1/admin/inventory/adjust")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.code", is("ACCESS_DENIED")));
    }

    @Test
    @DisplayName("US-04.4 T-04.4.1: Trừ tồn kho khi đơn hàng đặt thành công -> thành công HTTP 200, trừ onHand và tạo SALE tx")
    void deductOrderInventory_success_returns200() throws Exception {
        OrderInventoryDeductionRequest request = new OrderInventoryDeductionRequest(
                101L,
                "ORD-101",
                List.of(new OrderItemStockRequest(iphone128.getId(), 3)),
                "Xuất kho đơn hàng 101"
        );

        mockMvc.perform(post("/api/v1/admin/inventory/deduct-order")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Trừ tồn kho cho đơn hàng thành công")));

        Inventory updatedInv = inventoryRepository.findByVariantId(iphone128.getId()).orElseThrow();
        assertThat(updatedInv.getQuantityOnHand()).isEqualTo(17);
        assertThat(updatedInv.getAvailableQuantity()).isEqualTo(15);

        ProductVariant updatedVariant = productVariantRepository.findById(iphone128.getId()).orElseThrow();
        assertThat(updatedVariant.getStockQuantity()).isEqualTo(17);

        var txs = inventoryTransactionRepository.findAll();
        var saleTx = txs.stream()
                .filter(t -> t.getTransactionType() == InventoryTransactionType.SALE)
                .findFirst()
                .orElseThrow();
        assertThat(saleTx.getQuantityChange()).isEqualTo(-3);
        assertThat(saleTx.getReferenceType()).isEqualTo("ORDER");
        assertThat(saleTx.getReferenceId()).isEqualTo(101L);
    }

    @Test
    @DisplayName("US-04.4 T-04.4.1: Trừ tồn kho khi số lượng yêu cầu vượt quá tồn khả dụng -> 400 INSUFFICIENT_STOCK")
    void deductOrderInventory_insufficientStock_returns400() throws Exception {
        // iphone128 has onHand=20, reserved=2 -> available=18. Request 19 units.
        OrderInventoryDeductionRequest request = new OrderInventoryDeductionRequest(
                102L,
                "ORD-102",
                List.of(new OrderItemStockRequest(iphone128.getId(), 19)),
                "Đơn hàng vượt số lượng tồn"
        );

        mockMvc.perform(post("/api/v1/admin/inventory/deduct-order")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.code", is("INSUFFICIENT_STOCK")));

        Inventory untouchedInv = inventoryRepository.findByVariantId(iphone128.getId()).orElseThrow();
        assertThat(untouchedInv.getQuantityOnHand()).isEqualTo(20);
    }

    @Test
    @DisplayName("US-04.4 T-04.4.1: Khách hàng gọi API deduct-order bị 403 Forbidden")
    void deductOrderInventory_customerForbidden_returns403() throws Exception {
        OrderInventoryDeductionRequest request = new OrderInventoryDeductionRequest(
                103L,
                "ORD-103",
                List.of(new OrderItemStockRequest(iphone128.getId(), 1)),
                "Khách hàng cố tình gọi API admin"
        );

        mockMvc.perform(post("/api/v1/admin/inventory/deduct-order")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.code", is("ACCESS_DENIED")));
    }

    @Test
    @DisplayName("US-04.4 T-04.4.2: Hoàn tồn kho khi đơn hàng bị huỷ -> thành công HTTP 200, tăng onHand và tạo CANCEL_RETURN tx")
    void restoreOrderInventory_success_returns200() throws Exception {
        OrderInventoryRestoreRequest request = new OrderInventoryRestoreRequest(
                104L,
                "ORD-104",
                List.of(new OrderItemStockRequest(iphone128.getId(), 2)),
                "Khách hàng huỷ đơn do đặt nhầm"
        );

        mockMvc.perform(post("/api/v1/admin/inventory/restore-order")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Hoàn tồn kho cho đơn hàng thành công")));

        Inventory updatedInv = inventoryRepository.findByVariantId(iphone128.getId()).orElseThrow();
        assertThat(updatedInv.getQuantityOnHand()).isEqualTo(22);
        assertThat(updatedInv.getAvailableQuantity()).isEqualTo(20);

        var txs = inventoryTransactionRepository.findAll();
        var cancelTx = txs.stream()
                .filter(t -> t.getTransactionType() == InventoryTransactionType.CANCEL_RETURN)
                .findFirst()
                .orElseThrow();
        assertThat(cancelTx.getQuantityChange()).isEqualTo(2);
        assertThat(cancelTx.getReferenceType()).isEqualTo("ORDER");
        assertThat(cancelTx.getReferenceId()).isEqualTo(104L);
        assertThat(cancelTx.getNote()).isEqualTo("Khách hàng huỷ đơn do đặt nhầm");
    }

    @Test
    @DisplayName("US-04.4 T-04.4.2: Hoàn tồn kho để trống lý do -> 400 Validation Error")
    void restoreOrderInventory_blankReason_returns400() throws Exception {
        OrderInventoryRestoreRequest request = new OrderInventoryRestoreRequest(
                105L,
                "ORD-105",
                List.of(new OrderItemStockRequest(iphone128.getId(), 2)),
                ""
        );

        mockMvc.perform(post("/api/v1/admin/inventory/restore-order")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("US-04.4 T-04.4.2: Khách hàng gọi API restore-order bị 403 Forbidden")
    void restoreOrderInventory_customerForbidden_returns403() throws Exception {
        OrderInventoryRestoreRequest request = new OrderInventoryRestoreRequest(
                106L,
                "ORD-106",
                List.of(new OrderItemStockRequest(iphone128.getId(), 1)),
                "Khách hàng gọi restore"
        );

        mockMvc.perform(post("/api/v1/admin/inventory/restore-order")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.code", is("ACCESS_DENIED")));
    }
}

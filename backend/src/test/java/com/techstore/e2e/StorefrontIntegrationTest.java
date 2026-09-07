package com.techstore.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techstore.entity.Brand;
import com.techstore.entity.Category;
import com.techstore.entity.Inventory;
import com.techstore.entity.InventoryTransaction;
import com.techstore.entity.Product;
import com.techstore.entity.ProductImage;
import com.techstore.entity.ProductVariant;
import com.techstore.enums.InventoryTransactionType;
import com.techstore.enums.ProductStatus;
import com.techstore.enums.VariantStatus;
import com.techstore.repository.BrandRepository;
import com.techstore.repository.CategoryRepository;
import com.techstore.repository.InventoryRepository;
import com.techstore.repository.InventoryTransactionRepository;
import com.techstore.repository.ProductImageRepository;
import com.techstore.repository.ProductRepository;
import com.techstore.repository.ProductSpecificationRepository;
import com.techstore.repository.ProductVariantRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class StorefrontIntegrationTest {

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
    private CategoryRepository categoryRepository;

    @Autowired
    private BrandRepository brandRepository;

    private Brand brandApple;
    private Brand brandSamsung;
    private Category categoryPhone;
    private Category categoryLaptop;
    private Category inactiveCategory;

    @BeforeEach
    @AfterEach
    void cleanDatabase() {
        inventoryTransactionRepository.deleteAll();
        inventoryRepository.deleteAll();
        productSpecificationRepository.deleteAll();
        productImageRepository.deleteAll();
        productVariantRepository.deleteAll();
        productRepository.deleteAll();
        brandRepository.deleteAll();
        categoryRepository.deleteAll();
    }

    private void setupBaseData() {
        brandApple = brandRepository.save(new Brand("Apple", "Apple Inc.", "apple.png"));
        brandSamsung = brandRepository.save(new Brand("Samsung", "Samsung Group", "samsung.png"));

        Category cat1 = new Category("Điện thoại", "Điện thoại thông minh", null, "phone.png");
        cat1.updateDisplay(1, true);
        categoryPhone = categoryRepository.save(cat1);

        Category cat2 = new Category("Laptop", "Máy tính xách tay", null, "laptop.png");
        cat2.updateDisplay(2, true);
        categoryLaptop = categoryRepository.save(cat2);

        Category cat3 = new Category("Ẩn", "Danh mục ẩn", null, null);
        cat3.updateDisplay(99, false);
        inactiveCategory = categoryRepository.save(cat3);
    }

    @Test
    @DisplayName("US-05.1: Trang chủ storefront là public, chỉ trả sản phẩm ACTIVE và danh mục ACTIVE")
    void getHomeData_shouldReturnActiveProductsAndActiveCategoriesWithoutAuth() throws Exception {
        setupBaseData();

        // 1. Active product with 2 variants (one on sale) and primary image
        Product p1 = productRepository.save(new Product("iPhone 15 Pro", "Flagship Apple", brandApple, categoryPhone, ProductStatus.ACTIVE));
        ProductVariant v1_1 = productVariantRepository.save(new ProductVariant(p1, "IP15P-128", "Titan", "128GB",
                new BigDecimal("25000000"), new BigDecimal("28000000"), 10, VariantStatus.ACTIVE));
        ProductVariant v1_2 = productVariantRepository.save(new ProductVariant(p1, "IP15P-256", "Titan", "256GB",
                new BigDecimal("29000000"), null, 5, VariantStatus.ACTIVE));
        productImageRepository.save(new ProductImage(p1, null, "https://example.com/ip15p.jpg", true, 0));

        // 2. Draft product (should not be in storefront home)
        Product pDraft = productRepository.save(new Product("Galaxy S25 Ultra", "Upcoming", brandSamsung, categoryPhone, ProductStatus.DRAFT));
        productVariantRepository.save(new ProductVariant(pDraft, "S25U-256", "Đen", "256GB",
                new BigDecimal("30000000"), null, 20, VariantStatus.ACTIVE));

        // 3. Inactive product (should not be in storefront home)
        Product pInactive = productRepository.save(new Product("iPhone 11", "Old model", brandApple, categoryPhone, ProductStatus.INACTIVE));
        productVariantRepository.save(new ProductVariant(pInactive, "IP11-64", "Trắng", "64GB",
                new BigDecimal("10000000"), null, 5, VariantStatus.ACTIVE));

        // 4. Deleted product (should not be in storefront home)
        Product pDeleted = productRepository.save(new Product("iPhone 12", "Discontinued", brandApple, categoryPhone, ProductStatus.ACTIVE));
        pDeleted.softDelete();
        productRepository.save(pDeleted);

        mockMvc.perform(get("/api/v1/storefront/home"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.featuredProducts", hasSize(1)))
                .andExpect(jsonPath("$.data.featuredProducts[0].name").value("iPhone 15 Pro"))
                .andExpect(jsonPath("$.data.featuredProducts[0].minPrice").value(25000000.0))
                .andExpect(jsonPath("$.data.featuredProducts[0].maxPrice").value(29000000.0))
                .andExpect(jsonPath("$.data.featuredProducts[0].discountPercent").value(11))
                .andExpect(jsonPath("$.data.featuredProducts[0].totalStock").value(15))
                .andExpect(jsonPath("$.data.featuredProducts[0].hasStock").value(true))
                .andExpect(jsonPath("$.data.featuredProducts[0].thumbnailUrl").value("https://example.com/ip15p.jpg"))
                .andExpect(jsonPath("$.data.newArrivals", hasSize(1)))
                .andExpect(jsonPath("$.data.newArrivals[0].name").value("iPhone 15 Pro"))
                .andExpect(jsonPath("$.data.onSaleProducts", hasSize(1)))
                .andExpect(jsonPath("$.data.onSaleProducts[0].name").value("iPhone 15 Pro"))
                .andExpect(jsonPath("$.data.featuredCategories", hasSize(2)))
                .andExpect(jsonPath("$.data.featuredCategories[0].name").value("Điện thoại"))
                .andExpect(jsonPath("$.data.featuredCategories[1].name").value("Laptop"));
    }

    @Test
    @DisplayName("US-05.1: API GET /api/v1/products/featured sắp xếp theo lượt bán và createdAt")
    void getFeaturedProducts_shouldPrioritizeSalesCount() throws Exception {
        setupBaseData();

        // Product 1: without sales
        Product p1 = productRepository.save(new Product("Galaxy S24", "Samsung 2024", brandSamsung, categoryPhone, ProductStatus.ACTIVE));
        ProductVariant v1 = productVariantRepository.save(new ProductVariant(p1, "S24-128", "Tím", "128GB",
                new BigDecimal("18000000"), null, 20, VariantStatus.ACTIVE));

        // Product 2: with 5 sales
        Product p2 = productRepository.save(new Product("MacBook Air M3", "Apple laptop", brandApple, categoryLaptop, ProductStatus.ACTIVE));
        ProductVariant v2 = productVariantRepository.save(new ProductVariant(p2, "MBA-M3", "Xám", "256GB",
                new BigDecimal("27000000"), null, 15, VariantStatus.ACTIVE));
        Inventory inv2 = inventoryRepository.save(new Inventory(v2, 10, 0, 5));
        inventoryTransactionRepository.save(new InventoryTransaction(inv2, InventoryTransactionType.SALE, -5, "ORDER", 101L, "Order 1", null));

        mockMvc.perform(get("/api/v1/products/featured?limit=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].name").value("MacBook Air M3"))
                .andExpect(jsonPath("$.data[0].salesCount").value(5))
                .andExpect(jsonPath("$.data[1].name").value("Galaxy S24"))
                .andExpect(jsonPath("$.data[1].salesCount").value(0));
    }

    @Test
    @DisplayName("US-05.1: API GET /api/v1/products/on-sale chỉ trả sản phẩm có variant giảm giá")
    void getOnSaleProducts_shouldOnlyReturnDiscountedProducts() throws Exception {
        setupBaseData();

        // Product 1: has discount (originalPrice > price)
        Product p1 = productRepository.save(new Product("iPhone 15", "Apple", brandApple, categoryPhone, ProductStatus.ACTIVE));
        productVariantRepository.save(new ProductVariant(p1, "IP15-128", "Hồng", "128GB",
                new BigDecimal("19000000"), new BigDecimal("22000000"), 10, VariantStatus.ACTIVE));

        // Product 2: regular price (no discount)
        Product p2 = productRepository.save(new Product("iPad Air 5", "Apple tablet", brandApple, categoryLaptop, ProductStatus.ACTIVE));
        productVariantRepository.save(new ProductVariant(p2, "IPADA-64", "Xanh", "64GB",
                new BigDecimal("14000000"), null, 10, VariantStatus.ACTIVE));

        mockMvc.perform(get("/api/v1/products/on-sale"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].name").value("iPhone 15"))
                .andExpect(jsonPath("$.data[0].discountPercent").value(14));
    }

    @Test
    @DisplayName("US-05.1: Validate limit tham số đầu vào (1 <= limit <= 50)")
    void validateLimitParam_shouldReturnValidationErrorForOutOfRange() throws Exception {
        mockMvc.perform(get("/api/v1/storefront/home?limit=0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        mockMvc.perform(get("/api/v1/products/featured?limit=51"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("US-05.2: Lấy tất cả sản phẩm đang bán khi không truyền categoryId")
    void getProducts_withoutCategory_shouldReturnAllActiveProducts() throws Exception {
        setupBaseData();

        Product p1 = productRepository.save(new Product("iPhone 15", "Apple", brandApple, categoryPhone, ProductStatus.ACTIVE));
        productVariantRepository.save(new ProductVariant(p1, "IP15-128", "Đen", "128GB",
                new BigDecimal("20000000"), null, 5, VariantStatus.ACTIVE));

        Product p2 = productRepository.save(new Product("MacBook Air M2", "Laptop Apple", brandApple, categoryLaptop, ProductStatus.ACTIVE));
        productVariantRepository.save(new ProductVariant(p2, "MBA-M2", "Bạc", "256GB",
                new BigDecimal("24000000"), null, 8, VariantStatus.ACTIVE));

        // Inactive product should be excluded
        Product pInactive = productRepository.save(new Product("Galaxy Y", "Old", brandSamsung, categoryPhone, ProductStatus.INACTIVE));
        productVariantRepository.save(new ProductVariant(pInactive, "GY-1", "Trắng", "2GB",
                new BigDecimal("1000000"), null, 2, VariantStatus.ACTIVE));

        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(2)));
    }

    @Test
    @DisplayName("US-05.2: Lấy sản phẩm theo danh mục bao gồm cả danh mục con")
    void getProductsByCategory_shouldIncludeParentAndChildCategories() throws Exception {
        setupBaseData();

        // Create child category under categoryPhone
        Category childCategory = new Category("iPhone", "Dòng iPhone", categoryPhone, "iphone.png");
        childCategory.updateDisplay(1, true);
        childCategory = categoryRepository.save(childCategory);

        // Product in parent category
        Product pPhone = productRepository.save(new Product("Galaxy S24", "Samsung", brandSamsung, categoryPhone, ProductStatus.ACTIVE));
        productVariantRepository.save(new ProductVariant(pPhone, "S24-128", "Xám", "128GB",
                new BigDecimal("18000000"), null, 10, VariantStatus.ACTIVE));

        // Product in child category
        Product pIPhone = productRepository.save(new Product("iPhone 15 Pro", "Apple", brandApple, childCategory, ProductStatus.ACTIVE));
        productVariantRepository.save(new ProductVariant(pIPhone, "IP15P-128", "Titan", "128GB",
                new BigDecimal("25000000"), null, 5, VariantStatus.ACTIVE));

        // Product in different category (Laptop)
        Product pLaptop = productRepository.save(new Product("MacBook Pro", "Apple", brandApple, categoryLaptop, ProductStatus.ACTIVE));
        productVariantRepository.save(new ProductVariant(pLaptop, "MBP-14", "Xám", "512GB",
                new BigDecimal("45000000"), null, 3, VariantStatus.ACTIVE));

        // Query by parent category: should get both Galaxy S24 and iPhone 15 Pro
        mockMvc.perform(get("/api/v1/products?categoryId=" + categoryPhone.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(2)));

        // Query by child category: should only get iPhone 15 Pro
        mockMvc.perform(get("/api/v1/products?categoryId=" + childCategory.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].name").value("iPhone 15 Pro"));
    }

    @Test
    @DisplayName("US-05.2: Validate categoryId không tồn tại hoặc bị ẩn trả về lỗi 404 CATEGORY_NOT_FOUND")
    void getProductsByCategory_invalidCategory_shouldReturnError() throws Exception {
        setupBaseData();

        // Non-existent category
        mockMvc.perform(get("/api/v1/products?categoryId=999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("CATEGORY_NOT_FOUND"));

        // Inactive category
        mockMvc.perform(get("/api/v1/products?categoryId=" + inactiveCategory.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("CATEGORY_NOT_FOUND"));

        // Negative categoryId -> VALIDATION_ERROR
        mockMvc.perform(get("/api/v1/products?categoryId=-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}

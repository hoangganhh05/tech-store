package com.techstore.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techstore.entity.Brand;
import com.techstore.entity.Category;
import com.techstore.entity.Inventory;
import com.techstore.entity.InventoryTransaction;
import com.techstore.entity.Product;
import com.techstore.entity.ProductImage;
import com.techstore.entity.ProductSpecification;
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

    @Test
    @DisplayName("US-05.3: Tìm kiếm sản phẩm theo tên chính xác và không phân biệt hoa thường")
    void searchProducts_caseInsensitive_shouldReturnMatches() throws Exception {
        setupBaseData();

        Product p1 = productRepository.save(new Product("iPhone 15 Pro", "Flagship Apple", brandApple, categoryPhone, ProductStatus.ACTIVE));
        productVariantRepository.save(new ProductVariant(p1, "IP15P-1", "Titan", "128GB",
                new BigDecimal("25000000"), null, 10, VariantStatus.ACTIVE));

        Product p2 = productRepository.save(new Product("Samsung Galaxy S24", "Flagship Samsung", brandSamsung, categoryPhone, ProductStatus.ACTIVE));
        productVariantRepository.save(new ProductVariant(p2, "S24-1", "Xám", "128GB",
                new BigDecimal("18000000"), null, 10, VariantStatus.ACTIVE));

        // Search "iphone" in lowercase -> returns iPhone 15 Pro
        mockMvc.perform(get("/api/v1/products/search?q=iphone"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].name").value("iPhone 15 Pro"));

        // Search "GALAXY" in uppercase -> returns Samsung Galaxy S24
        mockMvc.perform(get("/api/v1/products/search?q=GALAXY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].name").value("Samsung Galaxy S24"));
    }

    @Test
    @DisplayName("US-05.3: Tìm kiếm sản phẩm không phân biệt dấu tiếng Việt")
    void searchProducts_accentInsensitive_shouldReturnMatches() throws Exception {
        setupBaseData();

        Product p1 = productRepository.save(new Product("Điện thoại Xiaomi 14", "Chính hãng DGW", brandApple, categoryPhone, ProductStatus.ACTIVE));
        productVariantRepository.save(new ProductVariant(p1, "XM14-1", "Đen", "256GB",
                new BigDecimal("15000000"), null, 10, VariantStatus.ACTIVE));

        Product p2 = productRepository.save(new Product("Bàn phím cơ AKKO", "Bàn phím cơ gõ êm", brandSamsung, categoryLaptop, ProductStatus.ACTIVE));
        productVariantRepository.save(new ProductVariant(p2, "AKKO-1", "Hồng", "TKL",
                new BigDecimal("1200000"), null, 15, VariantStatus.ACTIVE));

        // Search without accents: "dien thoai" matches "Điện thoại Xiaomi 14"
        mockMvc.perform(get("/api/v1/products/search?q=dien thoai"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].name").value("Điện thoại Xiaomi 14"));

        // Search without accents: "ban phim" matches "Bàn phím cơ AKKO"
        mockMvc.perform(get("/api/v1/products/search?q=ban phim"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].name").value("Bàn phím cơ AKKO"));

        // Search with accents: "Điện thoại" matches "Điện thoại Xiaomi 14"
        mockMvc.perform(get("/api/v1/products/search?q=Điện thoại"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].name").value("Điện thoại Xiaomi 14"));
    }

    @Test
    @DisplayName("US-05.3: Tìm kiếm theo thương hiệu và mô tả sản phẩm")
    void searchProducts_brandAndDescription_shouldReturnMatches() throws Exception {
        setupBaseData();

        Product p1 = productRepository.save(new Product("Tai nghe Pro", "Màn hình OLED và chống ồn", brandApple, categoryPhone, ProductStatus.ACTIVE));
        productVariantRepository.save(new ProductVariant(p1, "TNP-1", "Trắng", "Standard",
                new BigDecimal("5000000"), null, 8, VariantStatus.ACTIVE));

        // Search by brand name "Apple"
        mockMvc.perform(get("/api/v1/products/search?q=Apple"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].name").value("Tai nghe Pro"));

        // Search by description "chong on" (without accent)
        mockMvc.perform(get("/api/v1/products/search?q=chong on"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].name").value("Tai nghe Pro"));
    }

    @Test
    @DisplayName("US-05.3: Validate từ khoá rỗng, whitespace hoặc thiếu param trả về lỗi 400")
    void searchProducts_validationErrors_shouldReturnBadRequest() throws Exception {
        setupBaseData();

        // Empty query
        mockMvc.perform(get("/api/v1/products/search?q="))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        // Whitespace only query
        mockMvc.perform(get("/api/v1/products/search?q=   "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        // Missing q parameter
        mockMvc.perform(get("/api/v1/products/search"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("US-05.3: Không tìm thấy kết quả trả về mảng rỗng 200 OK và loại trừ sản phẩm ẩn/đã xoá")
    void searchProducts_emptyAndExcludedProducts_shouldBehaveCorrectly() throws Exception {
        setupBaseData();

        // Inactive product matching keyword
        Product inactiveProduct = productRepository.save(new Product("iPhone Cũ", "Apple", brandApple, categoryPhone, ProductStatus.INACTIVE));
        productVariantRepository.save(new ProductVariant(inactiveProduct, "IP-OLD", "Đen", "64GB",
                new BigDecimal("5000000"), null, 1, VariantStatus.ACTIVE));

        // Deleted product matching keyword
        Product deletedProduct = new Product("iPhone Xoá", "Apple", brandApple, categoryPhone, ProductStatus.ACTIVE);
        deletedProduct.softDelete();
        productRepository.save(deletedProduct);
        productVariantRepository.save(new ProductVariant(deletedProduct, "IP-DEL", "Đen", "64GB",
                new BigDecimal("5000000"), null, 1, VariantStatus.ACTIVE));

        // Search keyword "iPhone": should not return inactive or deleted product
        mockMvc.perform(get("/api/v1/products/search?q=iPhone"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(0)));

        // Non-existent search term
        mockMvc.perform(get("/api/v1/products/search?q=KhongTonTaiBatKyDau"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    @DisplayName("US-05.4: Lọc sản phẩm theo thương hiệu (multi-select)")
    void getProducts_filterByBrands_shouldReturnMatchingProducts() throws Exception {
        setupBaseData();

        Product pApple = productRepository.save(new Product("iPhone 15", "Apple Phone", brandApple, categoryPhone, ProductStatus.ACTIVE));
        productVariantRepository.save(new ProductVariant(pApple, "IP15-1", "Đen", "128GB",
                new BigDecimal("20000000"), null, 10, VariantStatus.ACTIVE));

        Product pSamsung = productRepository.save(new Product("Galaxy S24", "Samsung Phone", brandSamsung, categoryPhone, ProductStatus.ACTIVE));
        productVariantRepository.save(new ProductVariant(pSamsung, "S24-1", "Xám", "128GB",
                new BigDecimal("18000000"), null, 10, VariantStatus.ACTIVE));

        // Single brand filter
        mockMvc.perform(get("/api/v1/products?brandIds=" + brandApple.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].name").value("iPhone 15"));

        // Multi-select brands filter
        mockMvc.perform(get("/api/v1/products?brandIds=" + brandApple.getId() + "," + brandSamsung.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(2)));
    }

    @Test
    @DisplayName("US-05.4: Lọc sản phẩm theo khoảng giá (min-max)")
    void getProducts_filterByPriceRange_shouldReturnMatchingProducts() throws Exception {
        setupBaseData();

        Product pLow = productRepository.save(new Product("Tai nghe giá rẻ", "Tai nghe", brandApple, categoryPhone, ProductStatus.ACTIVE));
        productVariantRepository.save(new ProductVariant(pLow, "TN-1", "Trắng", "Std",
                new BigDecimal("2000000"), null, 10, VariantStatus.ACTIVE));

        Product pMid = productRepository.save(new Product("Galaxy A55", "Tầm trung", brandSamsung, categoryPhone, ProductStatus.ACTIVE));
        productVariantRepository.save(new ProductVariant(pMid, "A55-1", "Xanh", "128GB",
                new BigDecimal("9500000"), null, 10, VariantStatus.ACTIVE));

        Product pHigh = productRepository.save(new Product("MacBook Pro 16", "Cao cấp", brandApple, categoryLaptop, ProductStatus.ACTIVE));
        productVariantRepository.save(new ProductVariant(pHigh, "MBP16-1", "Bạc", "512GB",
                new BigDecimal("50000000"), null, 5, VariantStatus.ACTIVE));

        // priceMax = 5,000,000 -> only pLow
        mockMvc.perform(get("/api/v1/products?priceMax=5000000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].name").value("Tai nghe giá rẻ"));

        // priceMin = 5,000,000 and priceMax = 15,000,000 -> only pMid
        mockMvc.perform(get("/api/v1/products?priceMin=5000000&priceMax=15000000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].name").value("Galaxy A55"));

        // priceMin = 20,000,000 -> only pHigh
        mockMvc.perform(get("/api/v1/products?priceMin=20000000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].name").value("MacBook Pro 16"));
    }

    @Test
    @DisplayName("US-05.4: Lọc kết hợp danh mục, thương hiệu và khoảng giá")
    void getProducts_combinedFilters_shouldReturnMatchingProducts() throws Exception {
        setupBaseData();

        Product p1 = productRepository.save(new Product("iPhone 15 Pro", "Apple Phone", brandApple, categoryPhone, ProductStatus.ACTIVE));
        productVariantRepository.save(new ProductVariant(p1, "IP15P-1", "Titan", "128GB",
                new BigDecimal("25000000"), null, 10, VariantStatus.ACTIVE));

        Product p2 = productRepository.save(new Product("iPhone SE", "Apple Budget", brandApple, categoryPhone, ProductStatus.ACTIVE));
        productVariantRepository.save(new ProductVariant(p2, "IPSE-1", "Đỏ", "64GB",
                new BigDecimal("10000000"), null, 10, VariantStatus.ACTIVE));

        Product p3 = productRepository.save(new Product("MacBook Air", "Apple Laptop", brandApple, categoryLaptop, ProductStatus.ACTIVE));
        productVariantRepository.save(new ProductVariant(p3, "MBA-1", "Vàng", "256GB",
                new BigDecimal("25000000"), null, 5, VariantStatus.ACTIVE));

        Product p4 = productRepository.save(new Product("Galaxy S24", "Samsung Phone", brandSamsung, categoryPhone, ProductStatus.ACTIVE));
        productVariantRepository.save(new ProductVariant(p4, "S24-1", "Đen", "128GB",
                new BigDecimal("22000000"), null, 10, VariantStatus.ACTIVE));

        // Combined filter: categoryPhone + brandApple + priceMin=20,000,000
        // Should return only iPhone 15 Pro (p2 is too cheap, p3 is wrong category, p4 is Samsung)
        mockMvc.perform(get("/api/v1/products?categoryId=" + categoryPhone.getId()
                + "&brandIds=" + brandApple.getId()
                + "&priceMin=20000000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].name").value("iPhone 15 Pro"));
    }

    @Test
    @DisplayName("US-05.4: Validate lỗi khoảng giá hoặc thương hiệu không hợp lệ")
    void getProducts_filterValidation_shouldReturnBadRequest() throws Exception {
        setupBaseData();

        // priceMin > priceMax
        mockMvc.perform(get("/api/v1/products?priceMin=30000000&priceMax=20000000"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        // priceMin < 0
        mockMvc.perform(get("/api/v1/products?priceMin=-100"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        // priceMax < 0
        mockMvc.perform(get("/api/v1/products?priceMax=-500"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        // brandIds with invalid negative id
        mockMvc.perform(get("/api/v1/products?brandIds=-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("US-05.4: API public lấy danh sách thương hiệu phục vụ bộ lọc")
    void getFeaturedBrands_shouldReturnBrandList() throws Exception {
        setupBaseData();

        mockMvc.perform(get("/api/v1/storefront/brands"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].name").value("Apple"))
                .andExpect(jsonPath("$.data[1].name").value("Samsung"));
    }

    @Test
    @DisplayName("US-05.5: Sắp xếp theo giá tăng dần và giảm dần")
    void getProducts_sortByPrice_shouldReturnOrderedProducts() throws Exception {
        setupBaseData();

        Product pCheap = productRepository.save(new Product("Cáp sạc Type-C", "Cáp", brandApple, categoryPhone, ProductStatus.ACTIVE));
        productVariantRepository.save(new ProductVariant(pCheap, "CABLE-1", "Trắng", "1m",
                new BigDecimal("200000"), null, 50, VariantStatus.ACTIVE));

        Product pMid = productRepository.save(new Product("iPhone 13", "Phone", brandApple, categoryPhone, ProductStatus.ACTIVE));
        productVariantRepository.save(new ProductVariant(pMid, "IP13-1", "Xanh", "128GB",
                new BigDecimal("14000000"), null, 10, VariantStatus.ACTIVE));

        Product pExpensive = productRepository.save(new Product("MacBook Pro M3 Max", "Laptop", brandApple, categoryLaptop, ProductStatus.ACTIVE));
        productVariantRepository.save(new ProductVariant(pExpensive, "MBPM3-1", "Đen", "1TB",
                new BigDecimal("60000000"), null, 5, VariantStatus.ACTIVE));

        // Price ASC: Cáp sạc (200k) -> iPhone 13 (14M) -> MacBook (60M)
        mockMvc.perform(get("/api/v1/products?sortBy=price&sortDir=asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(3)))
                .andExpect(jsonPath("$.data[0].name").value("Cáp sạc Type-C"))
                .andExpect(jsonPath("$.data[1].name").value("iPhone 13"))
                .andExpect(jsonPath("$.data[2].name").value("MacBook Pro M3 Max"));

        // Price DESC: MacBook (60M) -> iPhone 13 (14M) -> Cáp sạc (200k)
        mockMvc.perform(get("/api/v1/products?sortBy=price&sortDir=desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(3)))
                .andExpect(jsonPath("$.data[0].name").value("MacBook Pro M3 Max"))
                .andExpect(jsonPath("$.data[1].name").value("iPhone 13"))
                .andExpect(jsonPath("$.data[2].name").value("Cáp sạc Type-C"));
    }

    @Test
    @DisplayName("US-05.5: Sắp xếp theo bán chạy nhất và mới nhất")
    void getProducts_sortBySalesAndNewest_shouldReturnOrderedProducts() throws Exception {
        setupBaseData();

        Product p1 = productRepository.save(new Product("Sản phẩm ít bán", "SP 1", brandApple, categoryPhone, ProductStatus.ACTIVE));
        ProductVariant v1 = productVariantRepository.save(new ProductVariant(p1, "SP1-V", "Đen", "64GB",
                new BigDecimal("5000000"), null, 20, VariantStatus.ACTIVE));
        Inventory inv1 = inventoryRepository.save(new Inventory(v1, 20, 0, 5));
        inventoryTransactionRepository.save(new InventoryTransaction(inv1, InventoryTransactionType.SALE, -2, null, null, "Sale 2", null));

        Product p2 = productRepository.save(new Product("Sản phẩm bán chạy nhất", "SP 2", brandSamsung, categoryPhone, ProductStatus.ACTIVE));
        ProductVariant v2 = productVariantRepository.save(new ProductVariant(p2, "SP2-V", "Bạc", "128GB",
                new BigDecimal("8000000"), null, 20, VariantStatus.ACTIVE));
        Inventory inv2 = inventoryRepository.save(new Inventory(v2, 20, 0, 5));
        inventoryTransactionRepository.save(new InventoryTransaction(inv2, InventoryTransactionType.SALE, -50, null, null, "Sale 50", null));

        // Sort by salesCount DESC: p2 (50 sold) should come before p1 (2 sold)
        mockMvc.perform(get("/api/v1/products?sortBy=salesCount&sortDir=desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].name").value("Sản phẩm bán chạy nhất"))
                .andExpect(jsonPath("$.data[1].name").value("Sản phẩm ít bán"));

        // Sort by createdAt DESC (newest first): p2 was created after p1
        mockMvc.perform(get("/api/v1/products?sortBy=createdAt&sortDir=desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].name").value("Sản phẩm bán chạy nhất"))
                .andExpect(jsonPath("$.data[1].name").value("Sản phẩm ít bán"));
    }

    @Test
    @DisplayName("US-05.5: Sắp xếp kết hợp với bộ lọc danh mục và thương hiệu")
    void getProducts_sortCombinedWithFilters_shouldReturnFilteredAndSorted() throws Exception {
        setupBaseData();

        Product pPhoneApple1 = productRepository.save(new Product("iPhone 13", "Phone", brandApple, categoryPhone, ProductStatus.ACTIVE));
        productVariantRepository.save(new ProductVariant(pPhoneApple1, "IP13-V", "Đen", "128GB",
                new BigDecimal("15000000"), null, 10, VariantStatus.ACTIVE));

        Product pPhoneApple2 = productRepository.save(new Product("iPhone 15", "Phone", brandApple, categoryPhone, ProductStatus.ACTIVE));
        productVariantRepository.save(new ProductVariant(pPhoneApple2, "IP15-V", "Hồng", "128GB",
                new BigDecimal("22000000"), null, 10, VariantStatus.ACTIVE));

        Product pPhoneSamsung = productRepository.save(new Product("Galaxy S24", "Phone", brandSamsung, categoryPhone, ProductStatus.ACTIVE));
        productVariantRepository.save(new ProductVariant(pPhoneSamsung, "S24-V", "Xám", "128GB",
                new BigDecimal("18000000"), null, 10, VariantStatus.ACTIVE));

        // Filter categoryPhone + brandApple + sortBy=price&sortDir=desc
        // Should only return iPhone 15 (22M) then iPhone 13 (15M)
        mockMvc.perform(get("/api/v1/products?categoryId=" + categoryPhone.getId()
                + "&brandIds=" + brandApple.getId()
                + "&sortBy=price&sortDir=desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].name").value("iPhone 15"))
                .andExpect(jsonPath("$.data[1].name").value("iPhone 13"));
    }

    @Test
    @DisplayName("US-05.5: Validate lỗi khi sortBy hoặc sortDir không hợp lệ")
    void getProducts_sortValidation_shouldReturnBadRequest() throws Exception {
        setupBaseData();

        // Invalid sortBy
        mockMvc.perform(get("/api/v1/products?sortBy=unknown_column"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        // Invalid sortDir
        mockMvc.perform(get("/api/v1/products?sortBy=price&sortDir=sideways"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("US-05.6: API GET /api/v1/products hỗ trợ phân trang chuẩn (page, size)")
    void getProducts_pagination_shouldReturnPaginatedResponse() throws Exception {
        setupBaseData();

        for (int i = 1; i <= 5; i++) {
            Product p = productRepository.save(new Product("Sản phẩm trang " + i, "SP " + i, brandApple, categoryPhone, ProductStatus.ACTIVE));
            productVariantRepository.save(new ProductVariant(p, "PAG-" + i, "Màu " + i, "128GB",
                    BigDecimal.valueOf(10000000 + i * 1000000), null, 10, VariantStatus.ACTIVE));
        }

        // Page 0, size 2 -> items 2, totalElements 5, totalPages 3, first true, last false
        mockMvc.perform(get("/api/v1/products?page=0&size=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items", hasSize(2)))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(2))
                .andExpect(jsonPath("$.data.totalElements").value(5))
                .andExpect(jsonPath("$.data.totalPages").value(3))
                .andExpect(jsonPath("$.data.first").value(true))
                .andExpect(jsonPath("$.data.last").value(false));

        // Page 1, size 2 -> items 2, first false, last false
        mockMvc.perform(get("/api/v1/products?page=1&size=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items", hasSize(2)))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.first").value(false))
                .andExpect(jsonPath("$.data.last").value(false));

        // Page 2, size 2 -> items 1, first false, last true
        mockMvc.perform(get("/api/v1/products?page=2&size=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.page").value(2))
                .andExpect(jsonPath("$.data.first").value(false))
                .andExpect(jsonPath("$.data.last").value(true));
    }

    @Test
    @DisplayName("US-05.6: Phân trang kết hợp với bộ lọc và sắp xếp")
    void getProducts_paginationWithFilterAndSort_shouldReturnFilteredAndSortedPage() throws Exception {
        setupBaseData();

        for (int i = 1; i <= 4; i++) {
            Product p = productRepository.save(new Product("SP Phone " + i, "Desc " + i, brandApple, categoryPhone, ProductStatus.ACTIVE));
            productVariantRepository.save(new ProductVariant(p, "PAG-F-" + i, "Đen", "128GB",
                    BigDecimal.valueOf(20000000 - i * 1000000), null, 10, VariantStatus.ACTIVE));
        }

        // Filter categoryPhone, sort price ASC (16M, 17M, 18M, 19M), page=0, size=2
        mockMvc.perform(get("/api/v1/products?categoryId=" + categoryPhone.getId()
                + "&sortBy=price&sortDir=asc&page=0&size=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items", hasSize(2)))
                .andExpect(jsonPath("$.data.totalElements").value(4))
                .andExpect(jsonPath("$.data.totalPages").value(2))
                .andExpect(jsonPath("$.data.items[0].minPrice").value(16000000))
                .andExpect(jsonPath("$.data.items[1].minPrice").value(17000000));
    }

    @Test
    @DisplayName("US-05.6: Validate dữ liệu phân trang không hợp lệ trả về lỗi 400")
    void getProducts_paginationValidation_shouldReturnBadRequest() throws Exception {
        setupBaseData();

        // page < 0
        mockMvc.perform(get("/api/v1/products?page=-1&size=10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        // size <= 0
        mockMvc.perform(get("/api/v1/products?page=0&size=0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        // size > 100
        mockMvc.perform(get("/api/v1/products?page=0&size=101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("US-06.1: Lấy chi tiết sản phẩm thành công với đầy đủ biến thể, ảnh và thông số kỹ thuật")
    void getProductDetail_success_shouldReturnFullProductWithVariantsImagesAndSpecs() throws Exception {
        setupBaseData();

        Product product = productRepository.save(new Product("iPhone 15 Pro Max", "Siêu phẩm flagship từ Apple với vỏ titan siêu bền và nhẹ.",
                brandApple, categoryPhone, ProductStatus.ACTIVE));

        productVariantRepository.save(new ProductVariant(product, "IP15PM-256-TN", "Titan Tự Nhiên", "256GB",
                BigDecimal.valueOf(29990000), BigDecimal.valueOf(34990000), 15, VariantStatus.ACTIVE));
        productVariantRepository.save(new ProductVariant(product, "IP15PM-512-BL", "Titan Xanh", "512GB",
                BigDecimal.valueOf(34990000), BigDecimal.valueOf(39990000), 5, VariantStatus.ACTIVE));

        productImageRepository.save(new ProductImage(product, null, "https://example.com/ip15pm-main.jpg", true, 1));
        productImageRepository.save(new ProductImage(product, null, "https://example.com/ip15pm-side.jpg", false, 2));

        productSpecificationRepository.save(new ProductSpecification(product, "Màn hình", "OLED 6.7 inch Super Retina XDR 120Hz", 1));
        productSpecificationRepository.save(new ProductSpecification(product, "Chip xử lý", "Apple A17 Pro (3nm)", 2));

        mockMvc.perform(get("/api/v1/products/" + product.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Lấy thông tin chi tiết sản phẩm thành công"))
                .andExpect(jsonPath("$.data.id").value(product.getId()))
                .andExpect(jsonPath("$.data.name").value("iPhone 15 Pro Max"))
                .andExpect(jsonPath("$.data.description").value("Siêu phẩm flagship từ Apple với vỏ titan siêu bền và nhẹ."))
                .andExpect(jsonPath("$.data.brandName").value("Apple"))
                .andExpect(jsonPath("$.data.categoryName").value("Điện thoại"))
                .andExpect(jsonPath("$.data.minPrice").value(29990000))
                .andExpect(jsonPath("$.data.maxPrice").value(34990000))
                .andExpect(jsonPath("$.data.originalPrice").value(34990000))
                .andExpect(jsonPath("$.data.totalStock").value(20))
                .andExpect(jsonPath("$.data.hasStock").value(true))
                .andExpect(jsonPath("$.data.variants", hasSize(2)))
                .andExpect(jsonPath("$.data.availableColors", hasSize(2)))
                .andExpect(jsonPath("$.data.availableColors[0]").value("Titan Tự Nhiên"))
                .andExpect(jsonPath("$.data.availableColors[1]").value("Titan Xanh"))
                .andExpect(jsonPath("$.data.availableStorages", hasSize(2)))
                .andExpect(jsonPath("$.data.availableStorages[0]").value("256GB"))
                .andExpect(jsonPath("$.data.availableStorages[1]").value("512GB"))
                .andExpect(jsonPath("$.data.images", hasSize(2)))
                .andExpect(jsonPath("$.data.specifications", hasSize(2)))
                .andExpect(jsonPath("$.data.specifications[0].specKey").value("Màn hình"))
                .andExpect(jsonPath("$.data.specifications[0].specValue").value("OLED 6.7 inch Super Retina XDR 120Hz"))
                .andExpect(jsonPath("$.data.specifications[1].specKey").value("Chip xử lý"))
                .andExpect(jsonPath("$.data.specifications[1].specValue").value("Apple A17 Pro (3nm)"));
    }

    @Test
    @DisplayName("US-06.2: API trả cấu trúc biến thể và danh sách thuộc tính color/storage combination")
    void getProductDetail_variantAttributes_shouldReturnCombinationData() throws Exception {
        setupBaseData();

        Product product = productRepository.save(new Product("Samsung Galaxy S24 Ultra", "Flagship AI",
                brandSamsung, categoryPhone, ProductStatus.ACTIVE));

        // 3 variants: Gray-256GB (stock 10), Gray-512GB (stock 0), Violet-256GB (stock 5)
        productVariantRepository.save(new ProductVariant(product, "S24U-GR-256", "Xám Titan", "256GB",
                BigDecimal.valueOf(26990000), BigDecimal.valueOf(31990000), 10, VariantStatus.ACTIVE));
        productVariantRepository.save(new ProductVariant(product, "S24U-GR-512", "Xám Titan", "512GB",
                BigDecimal.valueOf(29990000), BigDecimal.valueOf(35990000), 0, VariantStatus.ACTIVE));
        productVariantRepository.save(new ProductVariant(product, "S24U-VT-256", "Tím Titan", "256GB",
                BigDecimal.valueOf(26990000), BigDecimal.valueOf(31990000), 5, VariantStatus.ACTIVE));

        mockMvc.perform(get("/api/v1/products/" + product.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.variants", hasSize(3)))
                .andExpect(jsonPath("$.data.availableColors", hasSize(2)))
                .andExpect(jsonPath("$.data.availableColors[0]").value("Xám Titan"))
                .andExpect(jsonPath("$.data.availableColors[1]").value("Tím Titan"))
                .andExpect(jsonPath("$.data.availableStorages", hasSize(2)))
                .andExpect(jsonPath("$.data.availableStorages[0]").value("256GB"))
                .andExpect(jsonPath("$.data.availableStorages[1]").value("512GB"))
                .andExpect(jsonPath("$.data.variants[0].sku").value("S24U-GR-256"))
                .andExpect(jsonPath("$.data.variants[0].stockQuantity").value(10))
                .andExpect(jsonPath("$.data.variants[1].sku").value("S24U-GR-512"))
                .andExpect(jsonPath("$.data.variants[1].stockQuantity").value(0))
                .andExpect(jsonPath("$.data.variants[2].sku").value("S24U-VT-256"))
                .andExpect(jsonPath("$.data.variants[2].stockQuantity").value(5));
    }

    @Test
    @DisplayName("US-06.1: Truy cập sản phẩm không tồn tại trả về 404 PRODUCT_NOT_FOUND")
    void getProductDetail_notFound_shouldReturn404() throws Exception {
        mockMvc.perform(get("/api/v1/products/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Không tìm thấy sản phẩm"));
    }

    @Test
    @DisplayName("US-06.1: Truy cập sản phẩm ngừng bán (INACTIVE) trả về 404 thông báo ngừng kinh doanh")
    void getProductDetail_inactiveProduct_shouldReturn404Discontinued() throws Exception {
        setupBaseData();

        Product discontinuedProduct = productRepository.save(new Product("Old Phone Model", "Sản phẩm cũ",
                brandApple, categoryPhone, ProductStatus.INACTIVE));

        mockMvc.perform(get("/api/v1/products/" + discontinuedProduct.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Sản phẩm đã ngừng kinh doanh"));
    }

    @Test
    @DisplayName("US-06.1: Validate ID sản phẩm không hợp lệ trả về 400 VALIDATION_ERROR")
    void getProductDetail_invalidId_shouldReturn400BadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/products/-5"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        mockMvc.perform(get("/api/v1/products/0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("US-06.3: API trả thông tin tồn kho biến thể trạng thái IN_STOCK (tồn > 5)")
    void getVariantStock_inStock_shouldReturnInStockStatus() throws Exception {
        setupBaseData();

        Product product = productRepository.save(new Product("iPhone 15 Pro", "Flagship",
                brandApple, categoryPhone, ProductStatus.ACTIVE));
        ProductVariant variant = productVariantRepository.save(new ProductVariant(product, "IP15P-128", "Titan Tự Nhiên", "128GB",
                BigDecimal.valueOf(25990000), BigDecimal.valueOf(28990000), 10, VariantStatus.ACTIVE));

        mockMvc.perform(get("/api/v1/products/" + product.getId() + "/variants/" + variant.getId() + "/stock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.variantId").value(variant.getId()))
                .andExpect(jsonPath("$.data.productId").value(product.getId()))
                .andExpect(jsonPath("$.data.sku").value("IP15P-128"))
                .andExpect(jsonPath("$.data.stockQuantity").value(10))
                .andExpect(jsonPath("$.data.stockStatus").value("IN_STOCK"))
                .andExpect(jsonPath("$.data.isAvailable").value(true));
    }

    @Test
    @DisplayName("US-06.3: API trả thông tin tồn kho biến thể trạng thái LOW_STOCK (1 <= tồn <= 5)")
    void getVariantStock_lowStock_shouldReturnLowStockStatus() throws Exception {
        setupBaseData();

        Product product = productRepository.save(new Product("iPhone 15 Pro Max", "Flagship",
                brandApple, categoryPhone, ProductStatus.ACTIVE));
        ProductVariant variant = productVariantRepository.save(new ProductVariant(product, "IP15PM-256", "Titan Xanh", "256GB",
                BigDecimal.valueOf(29990000), BigDecimal.valueOf(34990000), 3, VariantStatus.ACTIVE));

        mockMvc.perform(get("/api/v1/products/" + product.getId() + "/variants/" + variant.getId() + "/stock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.variantId").value(variant.getId()))
                .andExpect(jsonPath("$.data.productId").value(product.getId()))
                .andExpect(jsonPath("$.data.stockQuantity").value(3))
                .andExpect(jsonPath("$.data.stockStatus").value("LOW_STOCK"))
                .andExpect(jsonPath("$.data.isAvailable").value(true));
    }

    @Test
    @DisplayName("US-06.3: API trả thông tin tồn kho biến thể trạng thái OUT_OF_STOCK (tồn = 0)")
    void getVariantStock_outOfStock_shouldReturnOutOfStockStatus() throws Exception {
        setupBaseData();

        Product product = productRepository.save(new Product("iPhone 15 Plus", "Plus",
                brandApple, categoryPhone, ProductStatus.ACTIVE));
        ProductVariant variant = productVariantRepository.save(new ProductVariant(product, "IP15PL-128", "Hồng", "128GB",
                BigDecimal.valueOf(22990000), BigDecimal.valueOf(25990000), 0, VariantStatus.ACTIVE));

        mockMvc.perform(get("/api/v1/products/" + product.getId() + "/variants/" + variant.getId() + "/stock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.variantId").value(variant.getId()))
                .andExpect(jsonPath("$.data.productId").value(product.getId()))
                .andExpect(jsonPath("$.data.stockQuantity").value(0))
                .andExpect(jsonPath("$.data.stockStatus").value("OUT_OF_STOCK"))
                .andExpect(jsonPath("$.data.isAvailable").value(false));
    }

    @Test
    @DisplayName("US-06.3: Biến thể không thuộc về sản phẩm trả về 404 VARIANT_NOT_FOUND")
    void getVariantStock_mismatchProduct_shouldReturn404() throws Exception {
        setupBaseData();

        Product product1 = productRepository.save(new Product("Phone 1", "P1", brandApple, categoryPhone, ProductStatus.ACTIVE));
        Product product2 = productRepository.save(new Product("Phone 2", "P2", brandApple, categoryPhone, ProductStatus.ACTIVE));

        ProductVariant variant2 = productVariantRepository.save(new ProductVariant(product2, "P2-VAR", "Đen", "128GB",
                BigDecimal.valueOf(10000000), BigDecimal.valueOf(12000000), 5, VariantStatus.ACTIVE));

        mockMvc.perform(get("/api/v1/products/" + product1.getId() + "/variants/" + variant2.getId() + "/stock"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("PRODUCT_VARIANT_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Biến thể không thuộc về sản phẩm này"));
    }

    @Test
    @DisplayName("US-06.3: Validate ID sản phẩm hoặc biến thể không hợp lệ trả về 400")
    void getVariantStock_invalidIds_shouldReturn400() throws Exception {
        mockMvc.perform(get("/api/v1/products/0/variants/1/stock"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        mockMvc.perform(get("/api/v1/products/1/variants/-1/stock"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("US-06.3: Chi tiết sản phẩm trả kèm trường stockStatus trong từng biến thể")
    void getProductDetail_variantsIncludeStockStatus() throws Exception {
        setupBaseData();

        Product product = productRepository.save(new Product("Samsung Galaxy S24", "Flagship",
                brandSamsung, categoryPhone, ProductStatus.ACTIVE));

        productVariantRepository.save(new ProductVariant(product, "S24-128", "Vàng", "128GB",
                BigDecimal.valueOf(19990000), BigDecimal.valueOf(22990000), 15, VariantStatus.ACTIVE));
        productVariantRepository.save(new ProductVariant(product, "S24-256", "Đen", "256GB",
                BigDecimal.valueOf(22990000), BigDecimal.valueOf(25990000), 2, VariantStatus.ACTIVE));
        productVariantRepository.save(new ProductVariant(product, "S24-512", "Xám", "512GB",
                BigDecimal.valueOf(25990000), BigDecimal.valueOf(28990000), 0, VariantStatus.ACTIVE));

        mockMvc.perform(get("/api/v1/products/" + product.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.variants[0].stockStatus").value("IN_STOCK"))
                .andExpect(jsonPath("$.data.variants[1].stockStatus").value("LOW_STOCK"))
                .andExpect(jsonPath("$.data.variants[2].stockStatus").value("OUT_OF_STOCK"));
    }
}

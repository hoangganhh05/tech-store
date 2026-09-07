package com.techstore.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techstore.dto.request.AddToCartRequest;
import com.techstore.dto.request.UpdateCartItemRequest;
import com.techstore.entity.Brand;
import com.techstore.entity.Cart;
import com.techstore.entity.CartItem;
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
import com.techstore.repository.CartItemRepository;
import com.techstore.repository.CartRepository;
import com.techstore.repository.CategoryRepository;
import com.techstore.repository.InventoryRepository;
import com.techstore.repository.ProductRepository;
import com.techstore.repository.ProductVariantRepository;
import com.techstore.repository.RoleRepository;
import com.techstore.repository.UserRepository;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class CartIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

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
    private TokenIssuer tokenIssuer;

    private Category testCategory;
    private Brand testBrand;
    private Product testProduct;
    private ProductVariant testVariant;
    private User testUser;
    private String userToken;

    @BeforeEach
    void setUp() {
        cleanDb();

        Category cat1 = new Category("Điện thoại", "phone", null, "phone.png");
        cat1.updateDisplay(1, true);
        testCategory = categoryRepository.save(cat1);
        testBrand = brandRepository.save(new Brand("Apple", "Thương hiệu Apple", "https://example.com/logo.png"));

        testProduct = productRepository.save(new Product("iPhone 15 Pro", "Mô tả iPhone 15 Pro", testBrand, testCategory, ProductStatus.ACTIVE));

        testVariant = productVariantRepository.save(new ProductVariant(
                testProduct,
                "IP15P-TITAN-128",
                "Titan Tự Nhiên",
                "128GB",
                new BigDecimal("25990000.00"),
                new BigDecimal("28990000.00"),
                10,
                VariantStatus.ACTIVE
        ));

        inventoryRepository.save(new Inventory(testVariant, 10, 0, 5));

        Role customerRole = roleRepository.findByCode(RoleCode.CUSTOMER)
                .orElseGet(() -> roleRepository.save(new Role(RoleCode.CUSTOMER, "Khách hàng")));
        testUser = new User("customer@example.com", "hash", "Khách hàng Test", "0901234567");
        testUser.addRole(customerRole);
        testUser = userRepository.save(testUser);
        userToken = tokenIssuer.issue(testUser).accessToken();
    }

    @AfterEach
    void tearDown() {
        cleanDb();
    }

    private void cleanDb() {
        cartItemRepository.deleteAll();
        cartRepository.deleteAll();
        inventoryRepository.deleteAll();
        productVariantRepository.deleteAll();
        productRepository.deleteAll();
        brandRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("US-07.1: Khách vãng lai thêm sản phẩm vào giỏ với X-Session-Id thành công")
    void addToCart_guestUser_success() throws Exception {
        String sessionId = UUID.randomUUID().toString();
        AddToCartRequest request = new AddToCartRequest(testVariant.getId(), 2);

        mockMvc.perform(post("/api/v1/cart/items")
                        .header("X-Session-Id", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", equalTo(true)))
                .andExpect(jsonPath("$.data.totalItems", equalTo(2)))
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.items[0].variantId", equalTo(testVariant.getId().intValue())))
                .andExpect(jsonPath("$.data.items[0].productName", equalTo("iPhone 15 Pro")))
                .andExpect(jsonPath("$.data.items[0].color", equalTo("Titan Tự Nhiên")))
                .andExpect(jsonPath("$.data.items[0].storage", equalTo("128GB")))
                .andExpect(jsonPath("$.data.items[0].quantity", equalTo(2)))
                .andExpect(jsonPath("$.data.items[0].subtotal", equalTo(51980000.0)));
    }

    @Test
    @DisplayName("US-07.1: Người dùng đã đăng nhập thêm sản phẩm vào giỏ thành công")
    void addToCart_authenticatedUser_success() throws Exception {
        AddToCartRequest request = new AddToCartRequest(testVariant.getId(), 1);

        mockMvc.perform(post("/api/v1/cart/items")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", equalTo(true)))
                .andExpect(jsonPath("$.data.totalItems", equalTo(1)))
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.items[0].variantId", equalTo(testVariant.getId().intValue())))
                .andExpect(jsonPath("$.data.items[0].quantity", equalTo(1)));
    }

    @Test
    @DisplayName("US-07.1: Thêm cùng một biến thể vào giỏ sẽ cộng dồn số lượng, không tạo dòng trùng")
    void addToCart_existingVariant_accumulatesQuantity() throws Exception {
        String sessionId = UUID.randomUUID().toString();

        // Thêm lần 1: số lượng 2
        mockMvc.perform(post("/api/v1/cart/items")
                        .header("X-Session-Id", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddToCartRequest(testVariant.getId(), 2))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalItems", equalTo(2)))
                .andExpect(jsonPath("$.data.items", hasSize(1)));

        // Thêm lần 2: số lượng 3
        mockMvc.perform(post("/api/v1/cart/items")
                        .header("X-Session-Id", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddToCartRequest(testVariant.getId(), 3))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalItems", equalTo(5)))
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.items[0].quantity", equalTo(5)));
    }

    @Test
    @DisplayName("US-07.1: Thêm số lượng vượt quá tồn kho khả dụng trả về lỗi INSUFFICIENT_STOCK (400)")
    void addToCart_exceedsStock_returnsError() throws Exception {
        String sessionId = UUID.randomUUID().toString();
        // Tồn kho hiện có là 10, yêu cầu 11
        AddToCartRequest request = new AddToCartRequest(testVariant.getId(), 11);

        mockMvc.perform(post("/api/v1/cart/items")
                        .header("X-Session-Id", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", equalTo("INSUFFICIENT_STOCK")));
    }

    @Test
    @DisplayName("US-07.1: Cộng dồn số lượng vượt tồn kho khả dụng trả về lỗi INSUFFICIENT_STOCK")
    void addToCart_accumulatedExceedsStock_returnsError() throws Exception {
        String sessionId = UUID.randomUUID().toString();
        // Lần 1: thêm 8 (tồn 10) -> OK
        mockMvc.perform(post("/api/v1/cart/items")
                        .header("X-Session-Id", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddToCartRequest(testVariant.getId(), 8))))
                .andExpect(status().isOk());

        // Lần 2: thêm tiếp 3 (8 + 3 = 11 > 10) -> Lỗi
        mockMvc.perform(post("/api/v1/cart/items")
                        .header("X-Session-Id", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddToCartRequest(testVariant.getId(), 3))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", equalTo("INSUFFICIENT_STOCK")));
    }

    @Test
    @DisplayName("US-07.1: Thêm biến thể không tồn tại trả về 404")
    void addToCart_variantNotFound_returns404() throws Exception {
        AddToCartRequest request = new AddToCartRequest(999999L, 1);

        mockMvc.perform(post("/api/v1/cart/items")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", equalTo("PRODUCT_VARIANT_NOT_FOUND")));
    }

    @Test
    @DisplayName("US-07.1: Thêm biến thể có số lượng <= 0 trả về 400 Bad Request")
    void addToCart_invalidQuantity_returns400() throws Exception {
        AddToCartRequest request = new AddToCartRequest(testVariant.getId(), 0);

        mockMvc.perform(post("/api/v1/cart/items")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", equalTo("VALIDATION_ERROR")));
    }

    @Test
    @DisplayName("US-07.1: Lấy thông tin giỏ hàng qua GET /api/v1/cart")
    void getCart_returnsCorrectData() throws Exception {
        AddToCartRequest request = new AddToCartRequest(testVariant.getId(), 2);

        mockMvc.perform(post("/api/v1/cart/items")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/cart")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", equalTo(true)))
                .andExpect(jsonPath("$.data.totalItems", equalTo(2)))
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.items[0].productName", equalTo("iPhone 15 Pro")));
    }

    @Test
    @DisplayName("US-07.2: Cập nhật số lượng sản phẩm trong giỏ thành công và tự tính lại tổng tiền")
    void updateCartItemQuantity_success() throws Exception {
        // Thêm sản phẩm vào giỏ trước (số lượng 2)
        AddToCartRequest addReq = new AddToCartRequest(testVariant.getId(), 2);
        mockMvc.perform(post("/api/v1/cart/items")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addReq)))
                .andExpect(status().isOk());

        Cart cart = cartRepository.findByUserId(testUser.getId()).orElseThrow();
        CartItem cartItem = cartItemRepository.findByCartId(cart.getId()).get(0);

        // Cập nhật số lượng lên 5
        UpdateCartItemRequest updateReq = new UpdateCartItemRequest(5);
        mockMvc.perform(patch("/api/v1/cart/items/" + cartItem.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", equalTo(true)))
                .andExpect(jsonPath("$.data.totalItems", equalTo(5)))
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.items[0].quantity", equalTo(5)))
                .andExpect(jsonPath("$.data.items[0].subtotal", equalTo(129950000.0)))
                .andExpect(jsonPath("$.data.subtotal", equalTo(129950000.0)));
    }

    @Test
    @DisplayName("US-07.2: Khách vãng lai cập nhật số lượng qua X-Session-Id thành công")
    void updateCartItemQuantity_guestUser_success() throws Exception {
        String sessionId = UUID.randomUUID().toString();
        AddToCartRequest addReq = new AddToCartRequest(testVariant.getId(), 1);
        mockMvc.perform(post("/api/v1/cart/items")
                        .header("X-Session-Id", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addReq)))
                .andExpect(status().isOk());

        Cart cart = cartRepository.findBySessionId(sessionId).orElseThrow();
        CartItem cartItem = cartItemRepository.findByCartId(cart.getId()).get(0);

        UpdateCartItemRequest updateReq = new UpdateCartItemRequest(3);
        mockMvc.perform(patch("/api/v1/cart/items/" + cartItem.getId())
                        .header("X-Session-Id", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalItems", equalTo(3)))
                .andExpect(jsonPath("$.data.items[0].quantity", equalTo(3)));
    }

    @Test
    @DisplayName("US-07.2: Cập nhật số lượng vượt tồn kho khả dụng trả về lỗi INSUFFICIENT_STOCK (400)")
    void updateCartItemQuantity_exceedsStock_returnsError() throws Exception {
        AddToCartRequest addReq = new AddToCartRequest(testVariant.getId(), 2);
        mockMvc.perform(post("/api/v1/cart/items")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addReq)))
                .andExpect(status().isOk());

        Cart cart = cartRepository.findByUserId(testUser.getId()).orElseThrow();
        CartItem cartItem = cartItemRepository.findByCartId(cart.getId()).get(0);

        // Tồn kho là 10, cập nhật thành 11
        UpdateCartItemRequest updateReq = new UpdateCartItemRequest(11);
        mockMvc.perform(patch("/api/v1/cart/items/" + cartItem.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", equalTo("INSUFFICIENT_STOCK")));
    }

    @Test
    @DisplayName("US-07.2: Cập nhật số lượng không hợp lệ (<= 0) trả về VALIDATION_ERROR (400)")
    void updateCartItemQuantity_invalidQuantity_returns400() throws Exception {
        AddToCartRequest addReq = new AddToCartRequest(testVariant.getId(), 2);
        mockMvc.perform(post("/api/v1/cart/items")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addReq)))
                .andExpect(status().isOk());

        Cart cart = cartRepository.findByUserId(testUser.getId()).orElseThrow();
        CartItem cartItem = cartItemRepository.findByCartId(cart.getId()).get(0);

        UpdateCartItemRequest updateReq = new UpdateCartItemRequest(0);
        mockMvc.perform(patch("/api/v1/cart/items/" + cartItem.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", equalTo("VALIDATION_ERROR")));
    }

    @Test
    @DisplayName("US-07.2: Cập nhật dòng sản phẩm không tồn tại trả về CART_ITEM_NOT_FOUND (404)")
    void updateCartItemQuantity_itemNotFound_returns404() throws Exception {
        AddToCartRequest addReq = new AddToCartRequest(testVariant.getId(), 2);
        mockMvc.perform(post("/api/v1/cart/items")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addReq)))
                .andExpect(status().isOk());

        UpdateCartItemRequest updateReq = new UpdateCartItemRequest(3);
        mockMvc.perform(patch("/api/v1/cart/items/999999")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", equalTo("CART_ITEM_NOT_FOUND")));
    }
}


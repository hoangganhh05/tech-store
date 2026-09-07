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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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

    @Test
    @DisplayName("US-07.3: Xoá sản phẩm khỏi giỏ hàng thành công và cập nhật lại tổng tiền")
    void removeCartItem_success() throws Exception {
        AddToCartRequest addReq = new AddToCartRequest(testVariant.getId(), 3);
        mockMvc.perform(post("/api/v1/cart/items")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addReq)))
                .andExpect(status().isOk());

        Cart cart = cartRepository.findByUserId(testUser.getId()).orElseThrow();
        CartItem cartItem = cartItemRepository.findByCartId(cart.getId()).get(0);

        mockMvc.perform(delete("/api/v1/cart/items/" + cartItem.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", equalTo(true)))
                .andExpect(jsonPath("$.message", equalTo("Xoá sản phẩm khỏi giỏ hàng thành công")))
                .andExpect(jsonPath("$.data.totalItems", equalTo(0)))
                .andExpect(jsonPath("$.data.subtotal", equalTo(0)))
                .andExpect(jsonPath("$.data.items", hasSize(0)));
    }

    @Test
    @DisplayName("US-07.3: Khách vãng lai xoá sản phẩm qua X-Session-Id thành công")
    void removeCartItem_guestUser_success() throws Exception {
        String sessionId = UUID.randomUUID().toString();
        AddToCartRequest addReq = new AddToCartRequest(testVariant.getId(), 2);
        mockMvc.perform(post("/api/v1/cart/items")
                        .header("X-Session-Id", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addReq)))
                .andExpect(status().isOk());

        Cart cart = cartRepository.findBySessionId(sessionId).orElseThrow();
        CartItem cartItem = cartItemRepository.findByCartId(cart.getId()).get(0);

        mockMvc.perform(delete("/api/v1/cart/items/" + cartItem.getId())
                        .header("X-Session-Id", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", equalTo(true)))
                .andExpect(jsonPath("$.data.totalItems", equalTo(0)))
                .andExpect(jsonPath("$.data.items", hasSize(0)));
    }

    @Test
    @DisplayName("US-07.3: Xoá sản phẩm không tồn tại trả về CART_ITEM_NOT_FOUND (404)")
    void removeCartItem_itemNotFound_returns404() throws Exception {
        mockMvc.perform(delete("/api/v1/cart/items/999999")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", equalTo("CART_NOT_FOUND"))); // Không có giỏ hàng -> CART_NOT_FOUND

        // Tạo giỏ trước rồi xoá item không tồn tại
        AddToCartRequest addReq = new AddToCartRequest(testVariant.getId(), 1);
        mockMvc.perform(post("/api/v1/cart/items")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addReq)))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/cart/items/888888")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", equalTo("CART_ITEM_NOT_FOUND")));
    }

    @Test
    @DisplayName("US-07.3: Xoá sản phẩm thuộc giỏ hàng khác trả về CART_ITEM_NOT_FOUND (404)")
    void removeCartItem_notOwner_returns404() throws Exception {
        // Tạo giỏ cho user A
        AddToCartRequest addReq = new AddToCartRequest(testVariant.getId(), 1);
        mockMvc.perform(post("/api/v1/cart/items")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addReq)))
                .andExpect(status().isOk());

        Cart cartA = cartRepository.findByUserId(testUser.getId()).orElseThrow();
        CartItem itemA = cartItemRepository.findByCartId(cartA.getId()).get(0);

        // Khách B với session khác cố xoá item của user A
        String sessionIdB = UUID.randomUUID().toString();
        // Tạo giỏ B
        AddToCartRequest addReqB = new AddToCartRequest(testVariant.getId(), 2);
        mockMvc.perform(post("/api/v1/cart/items")
                        .header("X-Session-Id", sessionIdB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addReqB)))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/cart/items/" + itemA.getId())
                        .header("X-Session-Id", sessionIdB))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", equalTo("CART_ITEM_NOT_FOUND")));
    }

    @Test
    @DisplayName("US-07.4: Đơn hàng dưới 5 triệu áp dụng phí vận chuyển tiêu chuẩn 30.000đ")
    void calculateCartTotals_underThreshold_appliesStandardShipping() throws Exception {
        // Tạo biến thể phụ kiện giá rẻ 200.000đ
        Product accessory = productRepository.save(new Product("Cáp sạc USB-C", "Cáp sạc nhanh", testBrand, testCategory, ProductStatus.ACTIVE));
        ProductVariant cheapVariant = productVariantRepository.save(new ProductVariant(
                accessory,
                "CAB-USBC-1M",
                "Trắng",
                "1m",
                new BigDecimal("200000.00"),
                new BigDecimal("250000.00"),
                50,
                VariantStatus.ACTIVE
        ));
        inventoryRepository.save(new Inventory(cheapVariant, 50, 0, 5));

        AddToCartRequest addReq = new AddToCartRequest(cheapVariant.getId(), 2); // 400.000đ
        mockMvc.perform(post("/api/v1/cart/items")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.subtotal", equalTo(400000.0)))
                .andExpect(jsonPath("$.data.shippingFee", equalTo(30000)))
                .andExpect(jsonPath("$.data.discountAmount", equalTo(0)))
                .andExpect(jsonPath("$.data.total", equalTo(430000.0)));
    }

    @Test
    @DisplayName("US-07.4: Đơn hàng từ 5 triệu trở lên được miễn phí vận chuyển (0đ)")
    void calculateCartTotals_overThreshold_appliesFreeShipping() throws Exception {
        AddToCartRequest addReq = new AddToCartRequest(testVariant.getId(), 1); // 25.990.000đ >= 5.000.000đ
        mockMvc.perform(post("/api/v1/cart/items")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.subtotal", equalTo(25990000.0)))
                .andExpect(jsonPath("$.data.shippingFee", equalTo(0)))
                .andExpect(jsonPath("$.data.discountAmount", equalTo(0)))
                .andExpect(jsonPath("$.data.total", equalTo(25990000.0)));
    }

    @Test
    @DisplayName("US-07.4: Giỏ hàng trống có tạm tính, phí ship và tổng cộng đều bằng 0")
    void calculateCartTotals_emptyCart_zeroTotals() throws Exception {
        mockMvc.perform(get("/api/v1/cart")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalItems", equalTo(0)))
                .andExpect(jsonPath("$.data.subtotal", equalTo(0)))
                .andExpect(jsonPath("$.data.shippingFee", equalTo(0)))
                .andExpect(jsonPath("$.data.discountAmount", equalTo(0)))
                .andExpect(jsonPath("$.data.total", equalTo(0)));
    }

    @Test
    @DisplayName("US-07.5: Kiểm tra tồn kho toàn bộ giỏ hàng hợp lệ trả về valid = true")
    void validateCartStock_allInStock_returnsValidTrue() throws Exception {
        AddToCartRequest addReq = new AddToCartRequest(testVariant.getId(), 2);
        mockMvc.perform(post("/api/v1/cart/items")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addReq)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/cart/validate")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", equalTo(true)))
                .andExpect(jsonPath("$.data.valid", equalTo(true)))
                .andExpect(jsonPath("$.data.issues", hasSize(0)));

        mockMvc.perform(get("/api/v1/cart")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hasStockIssue", equalTo(false)))
                .andExpect(jsonPath("$.data.canCheckout", equalTo(true)))
                .andExpect(jsonPath("$.data.items[0].hasStockIssue", equalTo(false)));
    }

    @Test
    @DisplayName("US-07.5: Tồn kho giảm xuống dưới số lượng trong giỏ hàng trả về INSUFFICIENT_STOCK và hasStockIssue = true")
    void validateCartStock_stockReducedBelowCartQuantity_returnsInsufficientStockIssue() throws Exception {
        AddToCartRequest addReq = new AddToCartRequest(testVariant.getId(), 5);
        mockMvc.perform(post("/api/v1/cart/items")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addReq)))
                .andExpect(status().isOk());

        // Giả lập người khác mua làm tồn kho giảm từ 10 xuống còn 2
        Inventory inv = inventoryRepository.findByVariantId(testVariant.getId()).orElseThrow();
        inv.setQuantityOnHand(2);
        inventoryRepository.save(inv);

        mockMvc.perform(post("/api/v1/cart/validate")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", equalTo(true)))
                .andExpect(jsonPath("$.data.valid", equalTo(false)))
                .andExpect(jsonPath("$.data.issues", hasSize(1)))
                .andExpect(jsonPath("$.data.issues[0].issueType", equalTo("INSUFFICIENT_STOCK")))
                .andExpect(jsonPath("$.data.issues[0].requestedQuantity", equalTo(5)))
                .andExpect(jsonPath("$.data.issues[0].availableStock", equalTo(2)));

        mockMvc.perform(get("/api/v1/cart")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hasStockIssue", equalTo(true)))
                .andExpect(jsonPath("$.data.canCheckout", equalTo(false)))
                .andExpect(jsonPath("$.data.items[0].hasStockIssue", equalTo(true)))
                .andExpect(jsonPath("$.data.items[0].stockStatusMessage", containsString("Tồn kho không đủ")));
    }

    @Test
    @DisplayName("US-07.5: Tồn kho giảm về 0 trả về OUT_OF_STOCK và canCheckout = false")
    void validateCartStock_stockReducedToZero_returnsOutOfStockIssue() throws Exception {
        AddToCartRequest addReq = new AddToCartRequest(testVariant.getId(), 2);
        mockMvc.perform(post("/api/v1/cart/items")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addReq)))
                .andExpect(status().isOk());

        // Giả lập sản phẩm hết sạch hàng
        Inventory inv = inventoryRepository.findByVariantId(testVariant.getId()).orElseThrow();
        inv.setQuantityOnHand(0);
        inventoryRepository.save(inv);

        mockMvc.perform(post("/api/v1/cart/validate")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.valid", equalTo(false)))
                .andExpect(jsonPath("$.data.issues", hasSize(1)))
                .andExpect(jsonPath("$.data.issues[0].issueType", equalTo("OUT_OF_STOCK")))
                .andExpect(jsonPath("$.data.issues[0].availableStock", equalTo(0)));

        mockMvc.perform(get("/api/v1/cart")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hasStockIssue", equalTo(true)))
                .andExpect(jsonPath("$.data.canCheckout", equalTo(false)))
                .andExpect(jsonPath("$.data.items[0].stockStatusMessage", equalTo("Sản phẩm hiện đã hết hàng")));
    }

    @Test
    @DisplayName("US-07.5: Sản phẩm bị chuyển sang ngừng kinh doanh trả về INACTIVE_OR_DELETED")
    void validateCartStock_productInactive_returnsInactiveIssue() throws Exception {
        AddToCartRequest addReq = new AddToCartRequest(testVariant.getId(), 1);
        mockMvc.perform(post("/api/v1/cart/items")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addReq)))
                .andExpect(status().isOk());

        // Chuyển sản phẩm sang INACTIVE
        testProduct.setStatus(ProductStatus.INACTIVE);
        productRepository.save(testProduct);

        mockMvc.perform(post("/api/v1/cart/validate")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.valid", equalTo(false)))
                .andExpect(jsonPath("$.data.issues", hasSize(1)))
                .andExpect(jsonPath("$.data.issues[0].issueType", equalTo("INACTIVE_OR_DELETED")));
    }

    @Test
    @DisplayName("US-07.5: Giỏ hàng trống khi validate trả về valid = true")
    void validateCartStock_emptyCart_returnsValidTrue() throws Exception {
        mockMvc.perform(post("/api/v1/cart/validate")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.valid", equalTo(true)))
                .andExpect(jsonPath("$.data.issues", hasSize(0)));
    }
}


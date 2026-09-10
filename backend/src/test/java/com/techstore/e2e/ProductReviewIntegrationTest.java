package com.techstore.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techstore.dto.request.SubmitReviewRequest;
import com.techstore.entity.Brand;
import com.techstore.entity.Category;
import com.techstore.entity.Order;
import com.techstore.entity.OrderAddress;
import com.techstore.entity.OrderItem;
import com.techstore.entity.Product;
import com.techstore.entity.ProductVariant;
import com.techstore.entity.Review;
import com.techstore.entity.Role;
import com.techstore.entity.User;
import com.techstore.enums.PaymentMethod;
import com.techstore.enums.ProductStatus;
import com.techstore.enums.ReviewStatus;
import com.techstore.enums.RoleCode;
import com.techstore.repository.BrandRepository;
import com.techstore.repository.CategoryRepository;
import com.techstore.repository.OrderRepository;
import com.techstore.repository.ProductRepository;
import com.techstore.repository.ProductVariantRepository;
import com.techstore.repository.ReviewRepository;
import com.techstore.repository.RoleRepository;
import com.techstore.repository.UserRepository;
import com.techstore.security.TokenIssuer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ProductReviewIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired BrandRepository brands;
    @Autowired CategoryRepository categories;
    @Autowired ProductRepository products;
    @Autowired ProductVariantRepository variants;
    @Autowired OrderRepository orders;
    @Autowired ReviewRepository reviews;
    @Autowired TokenIssuer tokenIssuer;

    private User customer;
    private User otherCustomer;
    private String customerToken;
    private String otherCustomerToken;
    private Product product;
    private ProductVariant variant;

    @BeforeEach
    void setUp() {
        Role customerRole = roles.findByCode(RoleCode.CUSTOMER)
                .orElseGet(() -> roles.save(new Role(RoleCode.CUSTOMER, "Khách hàng")));

        customer = saveUser("review-customer@example.com", customerRole);
        otherCustomer = saveUser("review-other@example.com", customerRole);
        customerToken = tokenIssuer.issue(customer).accessToken();
        otherCustomerToken = tokenIssuer.issue(otherCustomer).accessToken();

        Brand brand = brands.saveAndFlush(new Brand("Apple", "apple", "Brand Apple"));
        Category category = categories.saveAndFlush(new Category("Điện thoại", "dien-thoai", null, "Mô tả"));
        product = products.saveAndFlush(new Product("iPhone 15 Pro", "Mô tả iPhone", brand, category, ProductStatus.ACTIVE));
        variant = variants.saveAndFlush(new ProductVariant(product, "IP15P-BLK-128", "Titan Đen", "128GB",
                new BigDecimal("25000000"), new BigDecimal("27000000"), 50, com.techstore.enums.VariantStatus.ACTIVE));
    }

    @Test
    void shouldSubmitReviewSuccessfullyWhenOrderIsCompleted() throws Exception {
        saveOrder(customer, "TS-REV-COMPLETED-1", "COMPLETED", variant.getId());

        SubmitReviewRequest request = new SubmitReviewRequest(5, "Máy dùng rất mượt mà, pin trâu!");

        mockMvc.perform(post("/api/v1/products/{productId}/reviews", product.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.rating").value(5))
                .andExpect(jsonPath("$.data.comment").value("Máy dùng rất mượt mà, pin trâu!"))
                .andExpect(jsonPath("$.data.userFullName").value(customer.getFullName()))
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        Optional<Review> savedReview = reviews.findByUserIdAndProductId(customer.getId(), product.getId());
        assertThat(savedReview).isPresent();
        assertThat(savedReview.get().getRating()).isEqualTo(5);
        assertThat(savedReview.get().getComment()).isEqualTo("Máy dùng rất mượt mà, pin trâu!");
        assertThat(savedReview.get().getStatus()).isEqualTo(ReviewStatus.APPROVED);
    }

    @Test
    void shouldUpdateExistingReviewWhenSubmittedAgain() throws Exception {
        saveOrder(customer, "TS-REV-UPDATE", "COMPLETED", variant.getId());

        SubmitReviewRequest firstRequest = new SubmitReviewRequest(4, "Khá tốt");
        mockMvc.perform(post("/api/v1/products/{productId}/reviews", product.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rating").value(4));

        Long initialReviewId = reviews.findByUserIdAndProductId(customer.getId(), product.getId()).orElseThrow().getId();

        SubmitReviewRequest updateRequest = new SubmitReviewRequest(5, "Đã cập nhật sau 1 tuần dùng, rất ưng ý!");
        mockMvc.perform(post("/api/v1/products/{productId}/reviews", product.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(initialReviewId))
                .andExpect(jsonPath("$.data.rating").value(5))
                .andExpect(jsonPath("$.data.comment").value("Đã cập nhật sau 1 tuần dùng, rất ưng ý!"));

        assertThat(reviews.count()).isEqualTo(1);
    }

    @Test
    void shouldRejectReviewWhenOrderIsNotCompleted() throws Exception {
        saveOrder(customer, "TS-REV-PENDING", "PENDING", variant.getId());

        SubmitReviewRequest request = new SubmitReviewRequest(5, "Mong chờ hàng về");

        mockMvc.perform(post("/api/v1/products/{productId}/reviews", product.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("REVIEW_NOT_ELIGIBLE"));
    }

    @Test
    void shouldRejectReviewWhenUserNeverPurchasedProduct() throws Exception {
        SubmitReviewRequest request = new SubmitReviewRequest(5, "Chưa mua nhưng đánh giá thử");

        mockMvc.perform(post("/api/v1/products/{productId}/reviews", product.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherCustomerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("REVIEW_NOT_ELIGIBLE"));
    }

    @Test
    void shouldRejectInvalidRating() throws Exception {
        saveOrder(customer, "TS-REV-INVALID", "COMPLETED", variant.getId());

        SubmitReviewRequest zeroRating = new SubmitReviewRequest(0, "0 sao");
        mockMvc.perform(post("/api/v1/products/{productId}/reviews", product.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(zeroRating)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        SubmitReviewRequest sixRating = new SubmitReviewRequest(6, "6 sao");
        mockMvc.perform(post("/api/v1/products/{productId}/reviews", product.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sixRating)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldRejectUnauthorizedReviewSubmission() throws Exception {
        SubmitReviewRequest request = new SubmitReviewRequest(5, "Ẩn danh");

        mockMvc.perform(post("/api/v1/products/{productId}/reviews", product.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnEligibilityAndMyReview() throws Exception {
        mockMvc.perform(get("/api/v1/products/{productId}/reviews/my-review", product.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.canReview").value(false))
                .andExpect(jsonPath("$.data.myReview").doesNotExist());

        saveOrder(customer, "TS-REV-ELIGIBLE", "COMPLETED", variant.getId());

        mockMvc.perform(get("/api/v1/products/{productId}/reviews/my-review", product.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.canReview").value(true))
                .andExpect(jsonPath("$.data.myReview").doesNotExist());

        reviews.saveAndFlush(new Review(customer, product, 5, "Tuyệt vời"));

        mockMvc.perform(get("/api/v1/products/{productId}/reviews/my-review", product.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.canReview").value(true))
                .andExpect(jsonPath("$.data.myReview.rating").value(5))
                .andExpect(jsonPath("$.data.myReview.comment").value("Tuyệt vời"));
    }

    @Test
    void shouldReturnOnlyApprovedReviewsWithAverageAndNewestFirstPagination() throws Exception {
        User thirdCustomer = saveUser("review-third@example.com", roles.findByCode(RoleCode.CUSTOMER).orElseThrow());

        Review oldestApproved = new Review(customer, product, 5, "Rất tốt");
        oldestApproved.setCreatedAt(Instant.parse("2026-09-01T10:00:00Z"));
        Review newestApproved = new Review(otherCustomer, product, 3, "Tạm ổn");
        newestApproved.setCreatedAt(Instant.parse("2026-09-03T10:00:00Z"));
        Review hidden = new Review(thirdCustomer, product, 1, "Không hiển thị");
        hidden.setStatus(ReviewStatus.HIDDEN);
        hidden.setCreatedAt(Instant.parse("2026-09-04T10:00:00Z"));
        reviews.saveAllAndFlush(java.util.List.of(oldestApproved, newestApproved, hidden));

        mockMvc.perform(get("/api/v1/products/{productId}/reviews", product.getId())
                        .param("page", "0")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.averageRating").value(4.0))
                .andExpect(jsonPath("$.data.totalReviews").value(2))
                .andExpect(jsonPath("$.data.reviews.items", hasSize(1)))
                .andExpect(jsonPath("$.data.reviews.items[0].userFullName").value(otherCustomer.getFullName()))
                .andExpect(jsonPath("$.data.reviews.items[0].rating").value(3))
                .andExpect(jsonPath("$.data.reviews.totalPages").value(2));

        mockMvc.perform(get("/api/v1/products/{productId}/reviews", product.getId())
                        .param("page", "1")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reviews.items[0].userFullName").value(customer.getFullName()))
                .andExpect(jsonPath("$.data.reviews.items[0].rating").value(5));
    }

    @Test
    void shouldValidatePublicReviewListParametersAndProduct() throws Exception {
        mockMvc.perform(get("/api/v1/products/-1/reviews"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        mockMvc.perform(get("/api/v1/products/{productId}/reviews", product.getId()).param("size", "51"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        mockMvc.perform(get("/api/v1/products/999999/reviews"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));
    }

    private User saveUser(String email, Role role) {
        User user = new User(email, "hash", "Nguyễn Văn Review", "0912345678");
        user.addRole(role);
        return users.saveAndFlush(user);
    }

    private Order saveOrder(User user, String number, String status, Long variantId) {
        Order order = new Order(number, user, PaymentMethod.COD,
                new BigDecimal("25000000"), BigDecimal.ZERO, new BigDecimal("30000"));
        if (!"PENDING".equals(status)) {
            order.updateStatus(status, user);
        }
        order.setShippingAddress(new OrderAddress(order, "Nguyễn Văn Review", "0912345678", "1 Duy Tân",
                "Dịch Vọng", "Cầu Giấy", "Hà Nội"));
        order.addItem(new OrderItem(variantId, "iPhone 15 Pro", "IP15P-BLK-128", "Titan Đen / 128GB",
                new BigDecimal("25000000"), 1));
        return orders.saveAndFlush(order);
    }
}

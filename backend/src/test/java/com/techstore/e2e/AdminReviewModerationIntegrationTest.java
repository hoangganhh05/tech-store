package com.techstore.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techstore.dto.request.UpdateReviewStatusRequest;
import com.techstore.entity.Brand;
import com.techstore.entity.Category;
import com.techstore.entity.Product;
import com.techstore.entity.Review;
import com.techstore.entity.Role;
import com.techstore.entity.User;
import com.techstore.enums.ProductStatus;
import com.techstore.enums.ReviewStatus;
import com.techstore.enums.RoleCode;
import com.techstore.repository.BrandRepository;
import com.techstore.repository.CategoryRepository;
import com.techstore.repository.ProductRepository;
import com.techstore.repository.ReviewRepository;
import com.techstore.repository.RoleRepository;
import com.techstore.repository.UserRepository;
import com.techstore.security.IssuedTokenPair;
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

import static org.hamcrest.Matchers.hasSize;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminReviewModerationIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired BrandRepository brands;
    @Autowired CategoryRepository categories;
    @Autowired ProductRepository products;
    @Autowired ReviewRepository reviews;
    @Autowired TokenIssuer tokenIssuer;

    private User admin;
    private User customer;
    private Product product;
    private String adminToken;
    private String customerToken;

    @BeforeEach
    void setUp() {
        Role adminRole = roles.findByCode(RoleCode.ADMIN)
                .orElseGet(() -> roles.save(new Role(RoleCode.ADMIN, "Quản trị viên")));
        Role customerRole = roles.findByCode(RoleCode.CUSTOMER)
                .orElseGet(() -> roles.save(new Role(RoleCode.CUSTOMER, "Khách hàng")));

        admin = new User("review-admin@example.com", "hash", "Review Admin", "0900000001");
        admin.addRole(adminRole);
        admin = users.saveAndFlush(admin);
        customer = new User("review-customer-admin@example.com", "hash", "Review Customer", "0900000002");
        customer.addRole(customerRole);
        customer = users.saveAndFlush(customer);
        adminToken = tokenIssuer.issue(admin).accessToken();
        customerToken = tokenIssuer.issue(customer).accessToken();

        Brand brand = brands.saveAndFlush(new Brand("Review Brand", "review-brand", "Brand"));
        Category category = categories.saveAndFlush(new Category("Review Category", "review-category", null, "Category"));
        product = products.saveAndFlush(new Product("Review Product", "Product", brand, category, ProductStatus.ACTIVE));
    }

    @Test
    void adminCanFilterReviewsAndChangeDisplayStatus() throws Exception {
        Review pending = new Review(customer, product, 5, "Chờ duyệt");
        pending.setStatus(ReviewStatus.PENDING);
        Review hidden = new Review(admin, product, 1, "Ẩn");
        hidden.setStatus(ReviewStatus.HIDDEN);
        reviews.saveAllAndFlush(java.util.List.of(pending, hidden));

        mockMvc.perform(get("/api/v1/admin/reviews")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.items[0].status").value("PENDING"));

        mockMvc.perform(patch("/api/v1/admin/reviews/{id}/status", pending.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateReviewStatusRequest(ReviewStatus.APPROVED))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        assertThat(reviews.findById(pending.getId()).orElseThrow().getStatus()).isEqualTo(ReviewStatus.APPROVED);
    }

    @Test
    void hidingReviewRemovesItFromPublicStorefront() throws Exception {
        Review review = reviews.saveAndFlush(new Review(customer, product, 4, "Sẽ bị ẩn"));

        mockMvc.perform(patch("/api/v1/admin/reviews/{id}/status", review.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateReviewStatusRequest(ReviewStatus.HIDDEN))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/products/{productId}/reviews", product.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalReviews").value(0))
                .andExpect(jsonPath("$.data.reviews.items", hasSize(0)));
    }

    @Test
    void nonAdminCannotModerateReviews() throws Exception {
        mockMvc.perform(get("/api/v1/admin/reviews")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void validatesUnknownReviewAndStatus() throws Exception {
        mockMvc.perform(get("/api/v1/admin/reviews")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .param("status", "UNKNOWN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        mockMvc.perform(patch("/api/v1/admin/reviews/999999/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateReviewStatusRequest(ReviewStatus.HIDDEN))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("REVIEW_NOT_FOUND"));
    }
}

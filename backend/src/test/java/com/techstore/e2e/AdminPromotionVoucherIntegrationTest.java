package com.techstore.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techstore.dto.request.PromotionRequest;
import com.techstore.dto.request.VoucherRequest;
import com.techstore.entity.Brand;
import com.techstore.entity.Category;
import com.techstore.entity.Product;
import com.techstore.entity.ProductVariant;
import com.techstore.entity.Role;
import com.techstore.entity.User;
import com.techstore.enums.DiscountType;
import com.techstore.enums.ProductStatus;
import com.techstore.enums.PromotionTargetType;
import com.techstore.enums.RoleCode;
import com.techstore.enums.VariantStatus;
import com.techstore.repository.BrandRepository;
import com.techstore.repository.CategoryRepository;
import com.techstore.repository.ProductRepository;
import com.techstore.repository.ProductVariantRepository;
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
import java.time.temporal.ChronoUnit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminPromotionVoucherIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired BrandRepository brands;
    @Autowired CategoryRepository categories;
    @Autowired ProductRepository products;
    @Autowired ProductVariantRepository variants;
    @Autowired TokenIssuer tokenIssuer;

    private String adminToken;
    private String customerToken;
    private Product product;
    private ProductVariant variant;
    private Category category;

    @BeforeEach
    void setUp() {
        Role adminRole = roles.findByCode(RoleCode.ADMIN).orElseGet(() -> roles.save(new Role(RoleCode.ADMIN, "Admin")));
        Role customerRole = roles.findByCode(RoleCode.CUSTOMER).orElseGet(() -> roles.save(new Role(RoleCode.CUSTOMER, "Customer")));
        User admin = new User("promo-admin@example.com", "hash", "Promo Admin", "0900000001"); admin.addRole(adminRole);
        User customer = new User("promo-customer@example.com", "hash", "Promo Customer", "0900000002"); customer.addRole(customerRole);
        adminToken = "Bearer " + tokenIssuer.issue(users.saveAndFlush(admin)).accessToken();
        customerToken = "Bearer " + tokenIssuer.issue(users.saveAndFlush(customer)).accessToken();
        Brand brand = brands.saveAndFlush(new Brand("Promo Brand", "promo-brand", "Brand"));
        category = categories.saveAndFlush(new Category("Promo Category", "promo-category", null, "Category"));
        product = products.saveAndFlush(new Product("Promo Phone", "Description", brand, category, ProductStatus.ACTIVE));
        variant = variants.saveAndFlush(new ProductVariant(product, "PROMO-SKU", "Đen", "128GB", new BigDecimal("1000000"), null, 10, VariantStatus.ACTIVE));
    }

    @Test
    void adminCanManageVoucherAndDuplicateCodesAreRejected() throws Exception {
        VoucherRequest request = new VoucherRequest("save10", "Giảm 10%", DiscountType.PERCENT, new BigDecimal("10"),
                new BigDecimal("100000"), BigDecimal.ZERO, 20, 1, Instant.now().minus(1, ChronoUnit.HOURS),
                Instant.now().plus(1, ChronoUnit.DAYS), true);

        String response = mockMvc.perform(post("/api/v1/admin/vouchers").header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.code").value("SAVE10"))
                .andReturn().getResponse().getContentAsString();
        Long id = objectMapper.readTree(response).path("data").path("id").asLong();

        mockMvc.perform(get("/api/v1/admin/vouchers").header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.items[0].code").value("SAVE10"));
        mockMvc.perform(post("/api/v1/admin/vouchers").header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VOUCHER_CODE_DUPLICATE"));
        mockMvc.perform(delete("/api/v1/admin/vouchers/{id}", id).header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void activePromotionChangesStorefrontPriceWithoutChangingVariantBasePrice() throws Exception {
        PromotionRequest request = new PromotionRequest("Flash sale", PromotionTargetType.VARIANT, null, variant.getId(), null,
                new BigDecimal("20"), Instant.now().minus(1, ChronoUnit.HOURS), Instant.now().plus(1, ChronoUnit.DAYS), true);
        mockMvc.perform(post("/api/v1/admin/promotions").header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.targetType").value("VARIANT"));

        mockMvc.perform(get("/api/v1/products/{id}", product.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.variants[0].price").value(800000))
                .andExpect(jsonPath("$.data.variants[0].originalPrice").value(1000000));
    }

    @Test
    void expiredPromotionAutomaticallyRestoresBasePrice() throws Exception {
        PromotionRequest request = new PromotionRequest("Expired sale", PromotionTargetType.PRODUCT, product.getId(), null, null,
                new BigDecimal("25"), Instant.now().minus(2, ChronoUnit.DAYS), Instant.now().minus(1, ChronoUnit.HOURS), true);
        mockMvc.perform(post("/api/v1/admin/promotions").header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/products/{id}", product.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.variants[0].price").value(1000000))
                .andExpect(jsonPath("$.data.discountPercent").value(0));
    }

    @Test
    void promotionMustSpecifyTargetForSelectedScope() throws Exception {
        PromotionRequest request = new PromotionRequest("Missing target", PromotionTargetType.PRODUCT, null, null, null,
                new BigDecimal("10"), Instant.now(), Instant.now().plus(1, ChronoUnit.DAYS), true);
        mockMvc.perform(post("/api/v1/admin/promotions").header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void categoryPromotionChangesAllChildVariants() throws Exception {
        PromotionRequest request = new PromotionRequest("Category sale", PromotionTargetType.CATEGORY, null, null, category.getId(),
                new BigDecimal("15"), Instant.now().minus(1, ChronoUnit.HOURS), Instant.now().plus(1, ChronoUnit.DAYS), true);
        mockMvc.perform(post("/api/v1/admin/promotions").header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/products/{id}", product.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.variants[0].price").value(850000))
                .andExpect(jsonPath("$.data.variants[0].originalPrice").value(1000000));
    }

    @Test
    void customerCannotAccessPromotionOrVoucherManagement() throws Exception {
        mockMvc.perform(get("/api/v1/admin/vouchers").header(HttpHeaders.AUTHORIZATION, customerToken))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        mockMvc.perform(get("/api/v1/admin/promotions").header(HttpHeaders.AUTHORIZATION, customerToken))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }
}

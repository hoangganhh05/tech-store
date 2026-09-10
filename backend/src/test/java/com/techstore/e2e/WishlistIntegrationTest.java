package com.techstore.e2e;

import com.techstore.entity.Brand;
import com.techstore.entity.Category;
import com.techstore.entity.Product;
import com.techstore.entity.Role;
import com.techstore.entity.User;
import com.techstore.entity.Wishlist;
import com.techstore.enums.ProductStatus;
import com.techstore.enums.RoleCode;
import com.techstore.repository.BrandRepository;
import com.techstore.repository.CategoryRepository;
import com.techstore.repository.ProductRepository;
import com.techstore.repository.RoleRepository;
import com.techstore.repository.UserRepository;
import com.techstore.repository.WishlistRepository;
import com.techstore.security.TokenIssuer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class WishlistIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired RoleRepository roles;
    @Autowired UserRepository users;
    @Autowired BrandRepository brands;
    @Autowired CategoryRepository categories;
    @Autowired ProductRepository products;
    @Autowired WishlistRepository wishlists;
    @Autowired TokenIssuer tokenIssuer;

    private User customer;
    private String customerToken;
    private Product product;

    @BeforeEach
    void setUp() {
        Role customerRole = roles.findByCode(RoleCode.CUSTOMER)
                .orElseGet(() -> roles.save(new Role(RoleCode.CUSTOMER, "Khách hàng")));
        customer = new User("wishlist-customer@example.com", "{noop}password", "Wishlist Customer", "0900000000");
        customer.addRole(customerRole);
        customer = users.saveAndFlush(customer);
        customerToken = tokenIssuer.issue(customer).accessToken();

        Brand brand = brands.saveAndFlush(new Brand("Apple", "apple", "Apple"));
        Category category = categories.saveAndFlush(new Category("Điện thoại", "dien-thoai", null, "Điện thoại"));
        product = products.saveAndFlush(new Product("iPhone 15", "Mô tả", brand, category, ProductStatus.ACTIVE));
    }

    @Test
    void shouldRequireCustomerAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/wishlist"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAddListAndRemoveWishlistProduct() throws Exception {
        mockMvc.perform(post("/api/v1/wishlist/{productId}", product.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.productId").value(product.getId()))
                .andExpect(jsonPath("$.data.favorite").value(true));

        mockMvc.perform(post("/api/v1/wishlist/{productId}", product.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken))
                .andExpect(status().isOk());

        assertThat(wishlists.count()).isEqualTo(1);

        mockMvc.perform(get("/api/v1/wishlist")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken)
                        .param("page", "0")
                        .param("size", "12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.items[0].id").value(product.getId()))
                .andExpect(jsonPath("$.data.totalElements").value(1));

        mockMvc.perform(delete("/api/v1/wishlist/{productId}", product.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.favorite").value(false));

        assertThat(wishlists.findByUserIdAndProductId(customer.getId(), product.getId())).isEmpty();
    }

    @Test
    void shouldRejectInvalidProductAndPageParameters() throws Exception {
        mockMvc.perform(post("/api/v1/wishlist/0")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/wishlist/999999")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/wishlist")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken)
                        .param("page", "-1"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/wishlist")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken)
                        .param("size", "101"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldListOnlyActiveProducts() throws Exception {
        Product inactiveProduct = products.saveAndFlush(
                new Product("Inactive phone", "Mô tả", product.getBrand(), product.getCategory(), ProductStatus.INACTIVE));
        wishlists.saveAndFlush(new Wishlist(customer, product));
        wishlists.saveAndFlush(new Wishlist(customer, inactiveProduct));

        mockMvc.perform(get("/api/v1/wishlist")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$.data.items[0].id").value(product.getId()));
    }
}

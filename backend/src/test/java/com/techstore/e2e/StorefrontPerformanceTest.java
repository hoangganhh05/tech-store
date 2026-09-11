package com.techstore.e2e;

import com.techstore.entity.Brand;
import com.techstore.entity.Category;
import com.techstore.entity.Product;
import com.techstore.entity.ProductVariant;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class StorefrontPerformanceTest {

    private static final int MEDIUM_DATASET_SIZE = 100;
    private static final int WARMUP_REQUESTS = 10;
    private static final int MEASURED_REQUESTS = 30;
    private static final long P95_TARGET_MILLIS = 500;

    @Autowired MockMvc mockMvc;
    @Autowired InventoryTransactionRepository inventoryTransactionRepository;
    @Autowired InventoryRepository inventoryRepository;
    @Autowired ProductSpecificationRepository productSpecificationRepository;
    @Autowired ProductImageRepository productImageRepository;
    @Autowired ProductVariantRepository productVariantRepository;
    @Autowired ProductRepository productRepository;
    @Autowired BrandRepository brandRepository;
    @Autowired CategoryRepository categoryRepository;

    @BeforeEach
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

    @Test
    void productListP95ShouldStayBelow500MillisecondsForMediumDataset() throws Exception {
        Brand brand = brandRepository.save(new Brand("Benchmark brand", "Performance test", null));
        Category category = new Category("Benchmark category", "Performance test", null, null);
        category.updateDisplay(1, true);
        category = categoryRepository.save(category);

        for (int i = 1; i <= MEDIUM_DATASET_SIZE; i++) {
            Product product = productRepository.save(new Product(
                    "Benchmark phone " + i,
                    "Phone benchmark product",
                    brand,
                    category,
                    ProductStatus.ACTIVE
            ));
            productVariantRepository.save(new ProductVariant(
                    product,
                    "BENCH-" + i,
                    "Black",
                    "128GB",
                    BigDecimal.valueOf(10_000_000L + i),
                    null,
                    20,
                    VariantStatus.ACTIVE
            ));
        }

        for (int i = 0; i < WARMUP_REQUESTS; i++) {
            requestProductPage();
            requestProductSearch();
        }

        List<Long> listElapsedMillis = new ArrayList<>(MEASURED_REQUESTS);
        List<Long> searchElapsedMillis = new ArrayList<>(MEASURED_REQUESTS);
        for (int i = 0; i < MEASURED_REQUESTS; i++) {
            long startedAt = System.nanoTime();
            requestProductPage();
            listElapsedMillis.add((System.nanoTime() - startedAt) / 1_000_000L);

            startedAt = System.nanoTime();
            requestProductSearch();
            searchElapsedMillis.add((System.nanoTime() - startedAt) / 1_000_000L);
        }

        long listP95 = p95(listElapsedMillis);
        long searchP95 = p95(searchElapsedMillis);
        System.out.printf("US-14.5 storefront benchmark: dataset=%d, requests=%d, list-p95=%dms, search-p95=%dms%n",
                MEDIUM_DATASET_SIZE, MEASURED_REQUESTS, listP95, searchP95);
        assertThat(listP95).as("product list p95 latency").isLessThan(P95_TARGET_MILLIS);
        assertThat(searchP95).as("product search p95 latency").isLessThan(P95_TARGET_MILLIS);
    }

    private void requestProductPage() throws Exception {
        mockMvc.perform(get("/api/v1/products?page=0&size=20"))
                .andExpect(status().isOk());
    }

    private void requestProductSearch() throws Exception {
        mockMvc.perform(get("/api/v1/products/search?q=phone"))
                .andExpect(status().isOk());
    }

    private long p95(List<Long> elapsedMillis) {
        elapsedMillis.sort(Comparator.naturalOrder());
        return elapsedMillis.get((int) Math.ceil(elapsedMillis.size() * 0.95) - 1);
    }
}

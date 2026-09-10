package com.techstore.service;

import com.techstore.entity.Category;
import com.techstore.entity.Product;
import com.techstore.entity.ProductVariant;
import com.techstore.entity.Promotion;
import com.techstore.enums.ProductStatus;
import com.techstore.enums.PromotionTargetType;
import com.techstore.enums.VariantStatus;
import com.techstore.repository.CategoryRepository;
import com.techstore.repository.ProductRepository;
import com.techstore.repository.ProductVariantRepository;
import com.techstore.repository.PromotionRepository;
import com.techstore.service.impl.PromotionServiceImpl;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class PromotionServiceImplTest {
    private final PromotionRepository promotions = mock(PromotionRepository.class);
    private final ProductRepository products = mock(ProductRepository.class);
    private final ProductVariantRepository variants = mock(ProductVariantRepository.class);
    private final CategoryRepository categories = mock(CategoryRepository.class);
    private final PromotionServiceImpl service = new PromotionServiceImpl(promotions, products, variants, categories);

    @Test
    void choosesHighestApplicableDiscountAcrossVariantAndProductPromotions() throws Exception {
        Product product = mock(Product.class);
        ProductVariant variant = mock(ProductVariant.class);
        when(product.getId()).thenReturn(7L);
        when(variant.getId()).thenReturn(11L);
        when(variant.getProduct()).thenReturn(product);
        when(variant.getPrice()).thenReturn(new BigDecimal("1000000"));
        when(variant.getOriginalPrice()).thenReturn(new BigDecimal("1200000"));

        Instant start = Instant.now().minusSeconds(60);
        Instant end = Instant.now().plusSeconds(60);
        Promotion productPromo = new Promotion("Product sale", PromotionTargetType.PRODUCT, product, null, null,
                new BigDecimal("20"), start, end, true);
        Promotion variantPromo = new Promotion("Variant sale", PromotionTargetType.VARIANT, null, variant, null,
                new BigDecimal("15"), start, end, true);
        when(promotions.findActiveAt(any())).thenReturn(List.of(productPromo, variantPromo));

        EffectivePrice price = service.getEffectivePrices(List.of(variant)).get(11L);

        assertThat(price.price()).isEqualByComparingTo("800000.00");
        assertThat(price.originalPrice()).isEqualByComparingTo("1200000");
        assertThat(price.promotionPercent()).isEqualTo(20);
    }

    @Test
    void appliesPromotionToDescendantCategoryAndLeavesUnmatchedVariantUnchanged() throws Exception {
        Category root = mock(Category.class);
        Category child = mock(Category.class);
        Product product = mock(Product.class);
        ProductVariant matching = mock(ProductVariant.class);
        ProductVariant unmatched = mock(ProductVariant.class);
        when(root.getId()).thenReturn(2L);
        when(child.getId()).thenReturn(3L);
        when(child.getParent()).thenReturn(root);
        when(product.getCategory()).thenReturn(child);
        when(matching.getId()).thenReturn(11L);
        when(matching.getProduct()).thenReturn(product);
        when(matching.getPrice()).thenReturn(new BigDecimal("500000"));
        when(unmatched.getId()).thenReturn(12L);
        when(unmatched.getPrice()).thenReturn(new BigDecimal("300000"));
        Promotion categoryPromo = new Promotion("Phones", PromotionTargetType.CATEGORY, null, null, root,
                new BigDecimal("10"), Instant.now().minusSeconds(60), Instant.now().plusSeconds(60), true);
        when(promotions.findActiveAt(any())).thenReturn(List.of(categoryPromo));

        Map<Long, EffectivePrice> result = service.getEffectivePrices(List.of(matching, unmatched));

        assertThat(result.get(11L).price()).isEqualByComparingTo("450000.00");
        assertThat(result.get(11L).promotionPercent()).isEqualTo(10);
        assertThat(result.get(12L).price()).isEqualByComparingTo("300000");
        assertThat(result.get(12L).promotionPercent()).isZero();
    }

    @Test
    void returnsEmptyResultForNullOrEmptyInputWithoutRepositoryCall() {
        assertThat(service.getEffectivePrices(null)).isEmpty();
        assertThat(service.getEffectivePrices(List.of())).isEmpty();
        verifyNoInteractions(promotions);
    }
}

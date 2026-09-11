package com.techstore.service;

import com.techstore.entity.Product;
import com.techstore.enums.InventoryTransactionType;
import com.techstore.enums.ProductStatus;
import com.techstore.repository.BrandRepository;
import com.techstore.repository.CategoryRepository;
import com.techstore.repository.InventoryTransactionRepository;
import com.techstore.repository.ProductImageRepository;
import com.techstore.repository.ProductRepository;
import com.techstore.repository.ProductSpecificationRepository;
import com.techstore.repository.ProductVariantRepository;
import com.techstore.service.impl.StorefrontProductServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StorefrontProductServiceImplTest {

    @Mock ProductRepository productRepository;
    @Mock ProductVariantRepository productVariantRepository;
    @Mock ProductImageRepository productImageRepository;
    @Mock ProductSpecificationRepository productSpecificationRepository;
    @Mock CategoryRepository categoryRepository;
    @Mock InventoryTransactionRepository inventoryTransactionRepository;
    @Mock BrandRepository brandRepository;
    @Mock PromotionService promotionService;

    @Test
    void mapsAProductListWithBatchQueriesInsteadOfNPlusOneCalls() {
        List<Product> products = LongStream.rangeClosed(1, 25)
                .mapToObj(id -> {
                    Product product = org.mockito.Mockito.mock(Product.class);
                    when(product.getId()).thenReturn(id);
                    return product;
                })
                .toList();
        when(productRepository.findByStatusAndIsDeletedFalseOrderByCreatedAtDesc(ProductStatus.ACTIVE))
                .thenReturn(products);
        when(productVariantRepository.findByProductIdInAndIsDeletedFalseOrderByCreatedAtAsc(anyCollection()))
                .thenReturn(List.of());
        when(productImageRepository.findByProductIdInOrderByIsPrimaryDescDisplayOrderAscIdAsc(anyCollection()))
                .thenReturn(List.of());
        when(inventoryTransactionRepository.sumSalesQuantityByProductIds(anyCollection(), eq(InventoryTransactionType.SALE)))
                .thenReturn(List.of());
        when(promotionService.getEffectivePrices(anyCollection())).thenReturn(Map.of());

        StorefrontProductService service = new StorefrontProductServiceImpl(
                productRepository,
                productVariantRepository,
                productImageRepository,
                productSpecificationRepository,
                categoryRepository,
                inventoryTransactionRepository,
                brandRepository,
                promotionService
        );

        assertThat(service.getProducts(null, null, null, null)).hasSize(25);
        verify(productVariantRepository, times(1))
                .findByProductIdInAndIsDeletedFalseOrderByCreatedAtAsc(anyCollection());
        verify(productImageRepository, times(1))
                .findByProductIdInOrderByIsPrimaryDescDisplayOrderAscIdAsc(anyCollection());
        verify(inventoryTransactionRepository, times(1))
                .sumSalesQuantityByProductIds(anyCollection(), eq(InventoryTransactionType.SALE));
    }
}

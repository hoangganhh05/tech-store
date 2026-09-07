package com.techstore.service.impl;

import com.techstore.dto.response.CategoryResponse;
import com.techstore.dto.response.StorefrontHomeResponse;
import com.techstore.dto.response.StorefrontProductResponse;
import com.techstore.entity.Category;
import com.techstore.entity.Product;
import com.techstore.entity.ProductImage;
import com.techstore.entity.ProductVariant;
import com.techstore.enums.ErrorCode;
import com.techstore.enums.InventoryTransactionType;
import com.techstore.enums.ProductStatus;
import com.techstore.enums.VariantStatus;
import com.techstore.exception.BusinessException;
import com.techstore.repository.CategoryRepository;
import com.techstore.repository.InventoryTransactionRepository;
import com.techstore.repository.ProductImageRepository;
import com.techstore.repository.ProductRepository;
import com.techstore.repository.ProductVariantRepository;
import com.techstore.service.StorefrontProductService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class StorefrontProductServiceImpl implements StorefrontProductService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductImageRepository productImageRepository;
    private final CategoryRepository categoryRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;

    public StorefrontProductServiceImpl(
            ProductRepository productRepository,
            ProductVariantRepository productVariantRepository,
            ProductImageRepository productImageRepository,
            CategoryRepository categoryRepository,
            InventoryTransactionRepository inventoryTransactionRepository
    ) {
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
        this.productImageRepository = productImageRepository;
        this.categoryRepository = categoryRepository;
        this.inventoryTransactionRepository = inventoryTransactionRepository;
    }

    @Override
    public StorefrontHomeResponse getHomeData(int limit) {
        int safeLimit = normalizeLimit(limit);
        List<StorefrontProductResponse> featured = getFeaturedProducts(safeLimit);
        List<StorefrontProductResponse> newArrivals = getNewArrivals(safeLimit);
        List<StorefrontProductResponse> onSale = getOnSaleProducts(safeLimit);
        List<CategoryResponse> categories = getFeaturedCategories();

        return new StorefrontHomeResponse(featured, newArrivals, onSale, categories);
    }

    @Override
    public List<StorefrontProductResponse> getFeaturedProducts(int limit) {
        int safeLimit = normalizeLimit(limit);
        List<Product> products = productRepository.findFeaturedProducts(
                ProductStatus.ACTIVE,
                InventoryTransactionType.SALE,
                PageRequest.of(0, safeLimit)
        );
        return mapToStorefrontProductResponses(products);
    }

    @Override
    public List<StorefrontProductResponse> getNewArrivals(int limit) {
        int safeLimit = normalizeLimit(limit);
        List<Product> products = productRepository.findByStatusAndIsDeletedFalseOrderByCreatedAtDesc(
                ProductStatus.ACTIVE,
                PageRequest.of(0, safeLimit)
        );
        return mapToStorefrontProductResponses(products);
    }

    @Override
    public List<StorefrontProductResponse> getOnSaleProducts(int limit) {
        int safeLimit = normalizeLimit(limit);
        List<Product> products = productRepository.findOnSaleProducts(
                ProductStatus.ACTIVE,
                VariantStatus.ACTIVE,
                PageRequest.of(0, safeLimit)
        );
        return mapToStorefrontProductResponses(products);
    }

    @Override
    public List<CategoryResponse> getFeaturedCategories() {
        List<Category> categories = categoryRepository.findByParentIsNullAndIsActiveTrueOrderByDisplayOrderAscNameAsc();
        return categories.stream().map(c -> new CategoryResponse(
                c.getId(),
                c.getName(),
                c.getDescription(),
                c.getParent() != null ? c.getParent().getId() : null,
                c.getParent() != null ? c.getParent().getName() : null,
                c.getImageUrl(),
                c.getDisplayOrder(),
                c.getIsActive(),
                c.getCreatedAt(),
                c.getUpdatedAt()
        )).toList();
    }

    @Override
    public List<StorefrontProductResponse> getProducts(Long categoryId) {
        List<Product> products;
        if (categoryId != null) {
            if (categoryId <= 0) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Mã danh mục không hợp lệ");
            }
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND, "Danh mục không tồn tại"));
            if (!Boolean.TRUE.equals(category.getIsActive())) {
                throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND, "Danh mục không tồn tại hoặc đã bị ẩn");
            }
            products = productRepository.findByCategoryOrParentCategoryIdAndStatus(categoryId, ProductStatus.ACTIVE);
        } else {
            products = productRepository.findByStatusAndIsDeletedFalseOrderByCreatedAtDesc(ProductStatus.ACTIVE);
        }
        return mapToStorefrontProductResponses(products);
    }

    private int normalizeLimit(int limit) {
        return Math.max(1, Math.min(limit, 50));
    }

    private List<StorefrontProductResponse> mapToStorefrontProductResponses(List<Product> products) {
        if (products.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> productIds = products.stream().map(Product::getId).toList();

        List<ProductVariant> variants = productVariantRepository.findByProductIdInAndIsDeletedFalseOrderByCreatedAtAsc(productIds);
        Map<Long, List<ProductVariant>> variantsByProductId = variants.stream()
                .collect(Collectors.groupingBy(v -> v.getProduct().getId()));

        List<ProductImage> images = productImageRepository.findByProductIdInOrderByIsPrimaryDescDisplayOrderAscIdAsc(productIds);
        Map<Long, List<ProductImage>> imagesByProductId = images.stream()
                .collect(Collectors.groupingBy(img -> img.getProduct().getId()));

        List<Object[]> salesData = inventoryTransactionRepository.sumSalesQuantityByProductIds(productIds, InventoryTransactionType.SALE);
        Map<Long, Long> salesByProductId = new HashMap<>();
        for (Object[] row : salesData) {
            if (row.length >= 2 && row[0] instanceof Long pid && row[1] instanceof Number count) {
                salesByProductId.put(pid, count.longValue());
            }
        }

        return products.stream().map(product -> {
            List<ProductVariant> pVariants = variantsByProductId.getOrDefault(product.getId(), Collections.emptyList())
                    .stream()
                    .filter(v -> v.getStatus() == VariantStatus.ACTIVE)
                    .toList();

            BigDecimal minPrice = BigDecimal.ZERO;
            BigDecimal maxPrice = BigDecimal.ZERO;
            BigDecimal originalPrice = null;
            int discountPercent = 0;
            int totalStock = 0;

            if (!pVariants.isEmpty()) {
                minPrice = pVariants.stream()
                        .map(ProductVariant::getPrice)
                        .min(BigDecimal::compareTo)
                        .orElse(BigDecimal.ZERO);
                maxPrice = pVariants.stream()
                        .map(ProductVariant::getPrice)
                        .max(BigDecimal::compareTo)
                        .orElse(BigDecimal.ZERO);

                totalStock = pVariants.stream()
                        .mapToInt(v -> v.getStockQuantity() != null ? Math.max(0, v.getStockQuantity()) : 0)
                        .sum();

                int maxDiscount = 0;
                BigDecimal bestOriginalPrice = null;

                for (ProductVariant v : pVariants) {
                    if (v.getOriginalPrice() != null && v.getOriginalPrice().compareTo(v.getPrice()) > 0) {
                        BigDecimal diff = v.getOriginalPrice().subtract(v.getPrice());
                        int pct = diff.multiply(BigDecimal.valueOf(100))
                                .divide(v.getOriginalPrice(), 0, RoundingMode.HALF_UP)
                                .intValue();
                        if (pct > maxDiscount) {
                            maxDiscount = pct;
                            bestOriginalPrice = v.getOriginalPrice();
                        }
                    }
                }

                if (maxDiscount > 0) {
                    discountPercent = maxDiscount;
                    originalPrice = bestOriginalPrice;
                }
            }

            boolean hasStock = totalStock > 0;

            List<ProductImage> pImages = imagesByProductId.getOrDefault(product.getId(), Collections.emptyList());
            String thumbnailUrl = pImages.isEmpty() ? null : pImages.get(0).getImageUrl();

            long salesCount = salesByProductId.getOrDefault(product.getId(), 0L);
            double rating = 5.0;

            return new StorefrontProductResponse(
                    product.getId(),
                    product.getName(),
                    product.getDescription(),
                    product.getBrand() != null ? product.getBrand().getId() : null,
                    product.getBrand() != null ? product.getBrand().getName() : null,
                    product.getCategory() != null ? product.getCategory().getId() : null,
                    product.getCategory() != null ? product.getCategory().getName() : null,
                    thumbnailUrl,
                    minPrice,
                    maxPrice,
                    originalPrice,
                    discountPercent,
                    totalStock,
                    hasStock,
                    salesCount,
                    rating,
                    product.getCreatedAt()
            );
        }).toList();
    }
}


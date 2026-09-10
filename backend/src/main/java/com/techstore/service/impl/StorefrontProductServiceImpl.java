package com.techstore.service.impl;

import com.techstore.dto.response.BrandResponse;
import com.techstore.dto.response.CategoryResponse;
import com.techstore.dto.response.PageResponse;
import com.techstore.dto.response.ProductImageResponse;
import com.techstore.dto.response.ProductSpecificationResponse;
import com.techstore.dto.response.ProductVariantResponse;
import com.techstore.dto.response.StorefrontHomeResponse;
import com.techstore.dto.response.StorefrontProductDetailResponse;
import com.techstore.dto.response.StorefrontProductResponse;
import com.techstore.dto.response.VariantStockResponse;
import com.techstore.entity.Category;
import com.techstore.entity.Product;
import com.techstore.entity.ProductImage;
import com.techstore.entity.ProductSpecification;
import com.techstore.entity.ProductVariant;
import com.techstore.enums.ErrorCode;
import com.techstore.enums.InventoryTransactionType;
import com.techstore.enums.ProductStatus;
import com.techstore.enums.VariantStatus;
import com.techstore.exception.BusinessException;
import com.techstore.repository.BrandRepository;
import com.techstore.repository.CategoryRepository;
import com.techstore.repository.InventoryTransactionRepository;
import com.techstore.repository.ProductImageRepository;
import com.techstore.repository.ProductRepository;
import com.techstore.repository.ProductSpecificationRepository;
import com.techstore.repository.ProductVariantRepository;
import com.techstore.service.StorefrontProductService;
import com.techstore.service.EffectivePrice;
import com.techstore.service.PromotionService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class StorefrontProductServiceImpl implements StorefrontProductService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductSpecificationRepository productSpecificationRepository;
    private final CategoryRepository categoryRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final BrandRepository brandRepository;
    private final PromotionService promotionService;

    public StorefrontProductServiceImpl(
            ProductRepository productRepository,
            ProductVariantRepository productVariantRepository,
            ProductImageRepository productImageRepository,
            ProductSpecificationRepository productSpecificationRepository,
            CategoryRepository categoryRepository,
            InventoryTransactionRepository inventoryTransactionRepository,
            BrandRepository brandRepository,
            PromotionService promotionService
    ) {
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
        this.productImageRepository = productImageRepository;
        this.productSpecificationRepository = productSpecificationRepository;
        this.categoryRepository = categoryRepository;
        this.inventoryTransactionRepository = inventoryTransactionRepository;
        this.brandRepository = brandRepository;
        this.promotionService = promotionService;
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
        List<Product> products = productRepository.findByStatusAndIsDeletedFalseOrderByCreatedAtDesc(ProductStatus.ACTIVE);
        return mapToStorefrontProductResponses(products).stream()
                .filter(product -> product.discountPercent() != null && product.discountPercent() > 0)
                .limit(safeLimit)
                .toList();
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
        return getProducts(categoryId, null, null, null);
    }

    @Override
    public List<StorefrontProductResponse> getProducts(
            Long categoryId,
            List<Long> brandIds,
            BigDecimal priceMin,
            BigDecimal priceMax
    ) {
        return getProducts(categoryId, brandIds, priceMin, priceMax, null, null);
    }

    @Override
    public List<StorefrontProductResponse> getProducts(
            Long categoryId,
            List<Long> brandIds,
            BigDecimal priceMin,
            BigDecimal priceMax,
            String sortBy,
            String sortDir
    ) {
        if (categoryId != null) {
            if (categoryId <= 0) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Mã danh mục không hợp lệ");
            }
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND, "Danh mục không tồn tại"));
            if (!Boolean.TRUE.equals(category.getIsActive())) {
                throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND, "Danh mục không tồn tại hoặc đã bị ẩn");
            }
        }

        if (priceMin != null && priceMin.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Giá tối thiểu không được âm");
        }
        if (priceMax != null && priceMax.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Giá tối đa không được âm");
        }
        if (priceMin != null && priceMax != null && priceMin.compareTo(priceMax) > 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Giá tối thiểu không được lớn hơn giá tối đa");
        }

        List<Long> validBrandIds = null;
        if (brandIds != null && !brandIds.isEmpty()) {
            for (Long bId : brandIds) {
                if (bId == null || bId <= 0) {
                    throw new BusinessException(ErrorCode.VALIDATION_ERROR, "ID thương hiệu không hợp lệ");
                }
            }
            validBrandIds = brandIds;
        }

        // Validate sortBy
        String normalizedSortBy = null;
        if (sortBy != null && !sortBy.trim().isEmpty()) {
            String raw = sortBy.trim().toLowerCase();
            if (raw.equals("price")) {
                normalizedSortBy = "price";
            } else if (raw.equals("createdat") || raw.equals("created_at") || raw.equals("newest")) {
                normalizedSortBy = "createdAt";
            } else if (raw.equals("sales") || raw.equals("salescount") || raw.equals("sales_count") || raw.equals("best_seller") || raw.equals("bestseller")) {
                normalizedSortBy = "salesCount";
            } else {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Tiêu chí sắp xếp không hợp lệ");
            }
        }

        // Validate sortDir
        String normalizedSortDir = null;
        if (sortDir != null && !sortDir.trim().isEmpty()) {
            String raw = sortDir.trim().toLowerCase();
            if (raw.equals("asc") || raw.equals("desc")) {
                normalizedSortDir = raw;
            } else {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Chiều sắp xếp không hợp lệ");
            }
        }

        List<Product> products;
        if (categoryId != null) {
            products = productRepository.findByCategoryOrParentCategoryIdAndStatus(categoryId, ProductStatus.ACTIVE);
        } else {
            products = productRepository.findByStatusAndIsDeletedFalseOrderByCreatedAtDesc(ProductStatus.ACTIVE);
        }

        if (validBrandIds != null) {
            final List<Long> filterBrandIds = validBrandIds;
            products = products.stream()
                    .filter(p -> p.getBrand() != null && filterBrandIds.contains(p.getBrand().getId()))
                    .toList();
        }

        List<StorefrontProductResponse> responses = mapToStorefrontProductResponses(products);

        if (priceMin != null || priceMax != null) {
            responses = responses.stream()
                    .filter(p -> {
                        if (priceMin != null && p.maxPrice().compareTo(priceMin) < 0) {
                            return false;
                        }
                        if (priceMax != null && p.minPrice().compareTo(priceMax) > 0) {
                            return false;
                        }
                        return true;
                    })
                    .toList();
        }

        if (normalizedSortBy != null) {
            List<StorefrontProductResponse> sorted = new java.util.ArrayList<>(responses);
            boolean isAsc = "asc".equalsIgnoreCase(normalizedSortDir != null ? normalizedSortDir : ("price".equals(normalizedSortBy) ? "asc" : "desc"));

            if ("price".equals(normalizedSortBy)) {
                if (isAsc) {
                    sorted.sort(Comparator.comparing(StorefrontProductResponse::minPrice, Comparator.nullsLast(BigDecimal::compareTo))
                            .thenComparing(StorefrontProductResponse::id));
                } else {
                    sorted.sort(Comparator.comparing(StorefrontProductResponse::minPrice, Comparator.nullsLast(BigDecimal::compareTo)).reversed()
                            .thenComparing(StorefrontProductResponse::id, Comparator.reverseOrder()));
                }
            } else if ("salesCount".equals(normalizedSortBy)) {
                if (isAsc) {
                    sorted.sort(Comparator.comparing((StorefrontProductResponse p) -> p.salesCount() == null ? 0L : p.salesCount())
                            .thenComparing(StorefrontProductResponse::id));
                } else {
                    sorted.sort(Comparator.comparing((StorefrontProductResponse p) -> p.salesCount() == null ? 0L : p.salesCount(), Comparator.reverseOrder())
                            .thenComparing((StorefrontProductResponse p) -> p.createdAt() == null ? java.time.Instant.EPOCH : p.createdAt(), Comparator.reverseOrder())
                            .thenComparing(StorefrontProductResponse::id));
                }
            } else if ("createdAt".equals(normalizedSortBy)) {
                if (isAsc) {
                    sorted.sort(Comparator.comparing((StorefrontProductResponse p) -> p.createdAt() == null ? java.time.Instant.EPOCH : p.createdAt())
                            .thenComparing(StorefrontProductResponse::id));
                } else {
                    sorted.sort(Comparator.comparing((StorefrontProductResponse p) -> p.createdAt() == null ? java.time.Instant.EPOCH : p.createdAt(), Comparator.reverseOrder())
                            .thenComparing(StorefrontProductResponse::id, Comparator.reverseOrder()));
                }
            }
            return sorted;
        }

        return responses;
    }

    @Override
    public PageResponse<StorefrontProductResponse> getPaginatedProducts(
            Long categoryId,
            List<Long> brandIds,
            BigDecimal priceMin,
            BigDecimal priceMax,
            String sortBy,
            String sortDir,
            int page,
            int size
    ) {
        if (page < 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Số trang không được âm");
        }
        if (size <= 0 || size > 100) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Kích thước trang phải từ 1 đến 100");
        }

        List<StorefrontProductResponse> allProducts = getProducts(categoryId, brandIds, priceMin, priceMax, sortBy, sortDir);
        long totalElements = allProducts.size();
        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / size);

        int fromIndex = Math.min(page * size, (int) totalElements);
        int toIndex = Math.min(fromIndex + size, (int) totalElements);
        List<StorefrontProductResponse> pagedItems = allProducts.subList(fromIndex, toIndex);

        boolean isFirst = page == 0;
        boolean isLast = totalPages == 0 || page >= totalPages - 1;

        return new PageResponse<>(pagedItems, page, size, totalElements, totalPages, isFirst, isLast);
    }

    @Override
    public List<BrandResponse> getFeaturedBrands() {
        return brandRepository.findAllByOrderByNameAsc()
                .stream()
                .map(BrandResponse::from)
                .toList();
    }

    private static final Pattern ACCENT_PATTERN = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    @Override
    public List<StorefrontProductResponse> searchProducts(String query) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }
        String trimmed = query.trim();
        String normalizedKeyword = removeAccents(trimmed).toLowerCase();

        List<Product> activeProducts = productRepository.findByStatusAndIsDeletedFalseOrderByCreatedAtDesc(ProductStatus.ACTIVE);

        List<Product> matched = activeProducts.stream()
                .filter(p -> matchesSearch(p, normalizedKeyword))
                .toList();

        return mapToStorefrontProductResponses(matched);
    }

    @Override
    public List<StorefrontProductResponse> getProductsByIds(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, Product> productsById = productRepository.findAllById(productIds).stream()
                .filter(product -> product.getStatus() == ProductStatus.ACTIVE && !product.isDeleted())
                .collect(Collectors.toMap(Product::getId, product -> product));
        List<Product> orderedProducts = productIds.stream()
                .map(productsById::get)
                .filter(java.util.Objects::nonNull)
                .toList();
        return mapToStorefrontProductResponses(orderedProducts);
    }

    private boolean matchesSearch(Product product, String normalizedKeyword) {
        if (product.getName() != null && removeAccents(product.getName()).toLowerCase().contains(normalizedKeyword)) {
            return true;
        }
        if (product.getDescription() != null && removeAccents(product.getDescription()).toLowerCase().contains(normalizedKeyword)) {
            return true;
        }
        if (product.getBrand() != null && product.getBrand().getName() != null
                && removeAccents(product.getBrand().getName()).toLowerCase().contains(normalizedKeyword)) {
            return true;
        }
        return false;
    }

    private String removeAccents(String input) {
        if (input == null) {
            return "";
        }
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        return ACCENT_PATTERN.matcher(normalized).replaceAll("")
                .replace('\u0111', 'd')
                .replace('\u0110', 'D')
                .replace('đ', 'd')
                .replace('Đ', 'd');
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
        Map<Long, EffectivePrice> effectivePrices = promotionService.getEffectivePrices(variants);
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
                        .map(variant -> getEffectivePrice(variant, effectivePrices).price())
                        .min(BigDecimal::compareTo)
                        .orElse(BigDecimal.ZERO);
                maxPrice = pVariants.stream()
                        .map(variant -> getEffectivePrice(variant, effectivePrices).price())
                        .max(BigDecimal::compareTo)
                        .orElse(BigDecimal.ZERO);

                totalStock = pVariants.stream()
                        .mapToInt(v -> v.getStockQuantity() != null ? Math.max(0, v.getStockQuantity()) : 0)
                        .sum();

                int maxDiscount = 0;
                BigDecimal bestOriginalPrice = null;

                for (ProductVariant v : pVariants) {
                    EffectivePrice effectivePrice = getEffectivePrice(v, effectivePrices);
                    if (effectivePrice.originalPrice() != null && effectivePrice.originalPrice().compareTo(effectivePrice.price()) > 0) {
                        BigDecimal diff = effectivePrice.originalPrice().subtract(effectivePrice.price());
                        int pct = diff.multiply(BigDecimal.valueOf(100))
                                .divide(effectivePrice.originalPrice(), 0, RoundingMode.HALF_UP)
                                .intValue();
                        if (pct > 0) pct = Math.max(1, pct);
                        if (pct > maxDiscount) {
                            maxDiscount = pct;
                            bestOriginalPrice = effectivePrice.originalPrice();
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

    private EffectivePrice getEffectivePrice(ProductVariant variant, Map<Long, EffectivePrice> prices) {
        EffectivePrice price = prices.get(variant.getId());
        return price == null
                ? new EffectivePrice(variant.getPrice(), variant.getOriginalPrice(), 0)
                : price;
    }

    @Override
    public StorefrontProductDetailResponse getProductDetail(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "ID sản phẩm không hợp lệ");
        }

        Product product = productRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "Không tìm thấy sản phẩm"));

        if (product.getStatus() == ProductStatus.INACTIVE) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "Sản phẩm đã ngừng kinh doanh");
        }

        List<ProductVariant> variants = productVariantRepository.findByProductIdAndIsDeletedFalseOrderByCreatedAtAsc(id);
        List<ProductVariant> activeVariants = variants.stream()
                .filter(v -> v.getStatus() == VariantStatus.ACTIVE)
                .toList();
        Map<Long, EffectivePrice> effectivePrices = promotionService.getEffectivePrices(activeVariants);
        List<ProductVariantResponse> variantResponses = variants.stream()
                .filter(v -> v.getStatus() == VariantStatus.ACTIVE)
                .map(variant -> {
                    EffectivePrice effectivePrice = getEffectivePrice(variant, effectivePrices);
                    return ProductVariantResponse.from(variant, effectivePrice.price(), effectivePrice.originalPrice());
                })
                .toList();

        List<ProductImage> images = productImageRepository.findByProductIdOrderByIsPrimaryDescDisplayOrderAscIdAsc(id);
        List<ProductImageResponse> imageResponses = images.stream()
                .map(ProductImageResponse::from)
                .toList();

        List<ProductSpecification> specs = productSpecificationRepository.findByProductIdOrderByDisplayOrderAscIdAsc(id);
        List<ProductSpecificationResponse> specResponses = specs.stream()
                .map(ProductSpecificationResponse::from)
                .toList();

        BigDecimal minPrice = BigDecimal.ZERO;
        BigDecimal maxPrice = BigDecimal.ZERO;
        BigDecimal originalPrice = null;
        int discountPercent = 0;
        int totalStock = 0;

        if (!activeVariants.isEmpty()) {
            minPrice = activeVariants.stream()
                    .map(variant -> getEffectivePrice(variant, effectivePrices).price())
                    .min(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);
            maxPrice = activeVariants.stream()
                    .map(variant -> getEffectivePrice(variant, effectivePrices).price())
                    .max(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);

            totalStock = activeVariants.stream()
                    .mapToInt(v -> v.getStockQuantity() != null ? Math.max(0, v.getStockQuantity()) : 0)
                    .sum();

            int maxDiscount = 0;
            BigDecimal bestOriginalPrice = null;

            for (ProductVariant v : activeVariants) {
                EffectivePrice effectivePrice = getEffectivePrice(v, effectivePrices);
                if (effectivePrice.originalPrice() != null && effectivePrice.originalPrice().compareTo(effectivePrice.price()) > 0) {
                    BigDecimal diff = effectivePrice.originalPrice().subtract(effectivePrice.price());
                    int pct = diff.multiply(BigDecimal.valueOf(100))
                            .divide(effectivePrice.originalPrice(), 0, RoundingMode.HALF_UP)
                            .intValue();
                    if (pct > 0) pct = Math.max(1, pct);
                    if (pct > maxDiscount) {
                        maxDiscount = pct;
                        bestOriginalPrice = effectivePrice.originalPrice();
                    }
                }
            }

            if (maxDiscount > 0) {
                discountPercent = maxDiscount;
                originalPrice = bestOriginalPrice;
            }
        }

        boolean hasStock = totalStock > 0;

        List<Object[]> salesData = inventoryTransactionRepository.sumSalesQuantityByProductIds(List.of(id), InventoryTransactionType.SALE);
        long salesCount = 0L;
        if (!salesData.isEmpty() && salesData.get(0).length >= 2 && salesData.get(0)[1] instanceof Number count) {
            salesCount = count.longValue();
        }

        double rating = 5.0;

        List<String> availableColors = activeVariants.stream()
                .map(ProductVariant::getColor)
                .filter(c -> c != null && !c.isBlank())
                .distinct()
                .toList();

        List<String> availableStorages = activeVariants.stream()
                .map(ProductVariant::getStorage)
                .filter(s -> s != null && !s.isBlank())
                .distinct()
                .toList();

        return new StorefrontProductDetailResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getBrand() != null ? product.getBrand().getId() : null,
                product.getBrand() != null ? product.getBrand().getName() : null,
                product.getCategory() != null ? product.getCategory().getId() : null,
                product.getCategory() != null ? product.getCategory().getName() : null,
                product.getStatus(),
                minPrice,
                maxPrice,
                originalPrice,
                discountPercent,
                totalStock,
                hasStock,
                salesCount,
                rating,
                variantResponses,
                availableColors,
                availableStorages,
                imageResponses,
                specResponses,
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }

    @Override
    public VariantStockResponse getVariantStock(Long productId, Long variantId) {
        if (productId == null || productId <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "ID sản phẩm không hợp lệ");
        }
        if (variantId == null || variantId <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "ID biến thể không hợp lệ");
        }

        Product product = productRepository.findByIdAndIsDeletedFalse(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "Không tìm thấy sản phẩm"));

        if (product.getStatus() == ProductStatus.INACTIVE) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "Sản phẩm đã ngừng kinh doanh");
        }

        ProductVariant variant = productVariantRepository.findByIdAndIsDeletedFalse(variantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND, "Không tìm thấy biến thể sản phẩm"));

        if (!variant.getProduct().getId().equals(productId)) {
            throw new BusinessException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND, "Biến thể không thuộc về sản phẩm này");
        }

        if (variant.getStatus() != VariantStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND, "Biến thể đã ngừng kinh doanh");
        }

        return VariantStockResponse.of(
                variant.getId(),
                product.getId(),
                variant.getSku(),
                variant.getStockQuantity()
        );
    }

    @Override
    public List<StorefrontProductResponse> getRelatedProducts(Long productId, int limit) {
        if (productId == null || productId <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "ID sản phẩm không hợp lệ");
        }
        if (limit <= 0 || limit > 50) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Số lượng sản phẩm gợi ý phải từ 1 đến 50");
        }

        Product product = productRepository.findByIdAndIsDeletedFalse(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "Không tìm thấy sản phẩm"));

        if (product.getStatus() == ProductStatus.INACTIVE) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "Sản phẩm đã ngừng kinh doanh");
        }

        Long categoryId = product.getCategory() != null ? product.getCategory().getId() : null;
        Long brandId = product.getBrand() != null ? product.getBrand().getId() : null;

        if (categoryId == null && brandId == null) {
            return Collections.emptyList();
        }

        List<Product> related = productRepository.findRelatedProducts(
                productId,
                categoryId,
                brandId,
                ProductStatus.ACTIVE,
                PageRequest.of(0, limit)
        );

        return mapToStorefrontProductResponses(related);
    }
}


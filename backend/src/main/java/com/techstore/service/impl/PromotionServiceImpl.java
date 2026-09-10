package com.techstore.service.impl;

import com.techstore.dto.request.PromotionRequest;
import com.techstore.dto.response.PageResponse;
import com.techstore.dto.response.PromotionResponse;
import com.techstore.entity.Category;
import com.techstore.entity.Product;
import com.techstore.entity.ProductVariant;
import com.techstore.entity.Promotion;
import com.techstore.enums.ErrorCode;
import com.techstore.enums.ProductStatus;
import com.techstore.enums.PromotionTargetType;
import com.techstore.enums.VariantStatus;
import com.techstore.exception.BusinessException;
import com.techstore.repository.CategoryRepository;
import com.techstore.repository.ProductRepository;
import com.techstore.repository.ProductVariantRepository;
import com.techstore.repository.PromotionRepository;
import com.techstore.service.EffectivePrice;
import com.techstore.service.PromotionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class PromotionServiceImpl implements PromotionService {
    private static final int MAX_PAGE_SIZE = 50;
    private final PromotionRepository promotions;
    private final ProductRepository products;
    private final ProductVariantRepository variants;
    private final CategoryRepository categories;

    public PromotionServiceImpl(PromotionRepository promotions, ProductRepository products,
                                ProductVariantRepository variants, CategoryRepository categories) {
        this.promotions = promotions;
        this.products = products;
        this.variants = variants;
        this.categories = categories;
    }

    @Override
    public PromotionResponse create(PromotionRequest request) {
        Promotion promotion = build(null, request);
        return PromotionResponse.from(promotions.save(promotion));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PromotionResponse> getAll(int page, int size) {
        validatePage(page, size);
        Page<Promotion> result = promotions.findAll(PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));
        return PageResponse.of(result.map(PromotionResponse::from));
    }

    @Override
    @Transactional(readOnly = true)
    public PromotionResponse getById(Long id) {
        return PromotionResponse.from(find(id));
    }

    @Override
    public PromotionResponse update(Long id, PromotionRequest request) {
        Promotion existing = find(id);
        Promotion updated = build(existing, request);
        return PromotionResponse.from(promotions.save(updated));
    }

    @Override
    public void delete(Long id) {
        promotions.delete(find(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, EffectivePrice> getEffectivePrices(Collection<ProductVariant> variantsToPrice) {
        Map<Long, EffectivePrice> result = new HashMap<>();
        if (variantsToPrice == null || variantsToPrice.isEmpty()) return result;

        List<Promotion> active = promotions.findActiveAt(Instant.now());
        for (ProductVariant variant : variantsToPrice) {
            if (variant == null || variant.getId() == null || variant.getPrice() == null) continue;
            BigDecimal bestPercent = active.stream()
                    .filter(promotion -> appliesTo(promotion, variant))
                    .map(Promotion::getDiscountPercent)
                    .filter(percent -> percent != null)
                    .max(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);
            BigDecimal effectivePrice = bestPercent.signum() == 0 ? variant.getPrice()
                    : variant.getPrice().multiply(BigDecimal.valueOf(100).subtract(bestPercent))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            BigDecimal originalPrice = variant.getOriginalPrice() != null
                    && variant.getOriginalPrice().compareTo(effectivePrice) > 0
                    ? variant.getOriginalPrice() : (bestPercent.signum() == 0 ? variant.getOriginalPrice() : variant.getPrice());
            result.put(variant.getId(), new EffectivePrice(effectivePrice, originalPrice,
                    bestPercent.setScale(0, RoundingMode.HALF_UP).intValue()));
        }
        return result;
    }

    private boolean appliesTo(Promotion promotion, ProductVariant variant) {
        return switch (promotion.getTargetType()) {
            case VARIANT -> promotion.getVariant() != null && promotion.getVariant().getId().equals(variant.getId());
            case PRODUCT -> promotion.getProduct() != null && variant.getProduct() != null
                    && promotion.getProduct().getId().equals(variant.getProduct().getId());
            case CATEGORY -> promotion.getCategory() != null && isInCategoryTree(
                    variant.getProduct() == null ? null : variant.getProduct().getCategory(), promotion.getCategory().getId());
        };
    }

    private boolean isInCategoryTree(Category category, Long targetCategoryId) {
        Category current = category;
        while (current != null) {
            if (targetCategoryId.equals(current.getId())) return true;
            current = current.getParent();
        }
        return false;
    }

    private Promotion build(Promotion existing, PromotionRequest request) {
        if (request == null || request.targetType() == null || request.discountPercent() == null
                || request.startsAt() == null || request.endsAt() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Dữ liệu chương trình khuyến mãi không hợp lệ");
        }
        if (request.name() == null || request.name().isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Tên chương trình không được để trống");
        }
        if (request.discountPercent().compareTo(BigDecimal.ZERO) <= 0
                || request.discountPercent().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Mức giảm phải lớn hơn 0% và không vượt quá 100%");
        }
        if (!request.endsAt().isAfter(request.startsAt())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Thời gian kết thúc phải sau thời gian bắt đầu");
        }
        Product product = null;
        ProductVariant variant = null;
        Category category = null;
        switch (request.targetType()) {
            case PRODUCT -> {
                requireTargetId(request.productId(), "Mã sản phẩm không hợp lệ");
                product = products.findByIdAndIsDeletedFalse(request.productId())
                    .filter(item -> item.getStatus() == ProductStatus.ACTIVE)
                    .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "Không tìm thấy sản phẩm đang hoạt động"));
            }
            case VARIANT -> {
                requireTargetId(request.variantId(), "Mã biến thể không hợp lệ");
                variant = variants.findByIdAndIsDeletedFalse(request.variantId())
                    .filter(item -> item.getStatus() == VariantStatus.ACTIVE && item.getProduct().getStatus() == ProductStatus.ACTIVE)
                    .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND, "Không tìm thấy biến thể đang hoạt động"));
            }
            case CATEGORY -> {
                requireTargetId(request.categoryId(), "Mã danh mục không hợp lệ");
                category = categories.findById(request.categoryId())
                    .filter(item -> Boolean.TRUE.equals(item.getIsActive()))
                    .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND, "Không tìm thấy danh mục đang hoạt động"));
            }
        }
        boolean active = request.active() == null || request.active();
        if (existing == null) {
            return new Promotion(request.name(), request.targetType(), product, variant, category, request.discountPercent(),
                    request.startsAt(), request.endsAt(), active);
        }
        existing.update(request.name(), request.targetType(), product, variant, category, request.discountPercent(),
                request.startsAt(), request.endsAt(), active);
        return existing;
    }

    private Promotion find(Long id) {
        if (id == null || id < 1) throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Mã chương trình không hợp lệ");
        return promotions.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.PROMOTION_NOT_FOUND, "Không tìm thấy chương trình khuyến mãi"));
    }

    private void requireTargetId(Long id, String message) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, message);
        }
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Thông tin phân trang không hợp lệ");
        }
    }
}

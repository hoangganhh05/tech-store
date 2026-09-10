package com.techstore.dto.response;

import com.techstore.entity.Promotion;
import com.techstore.enums.PromotionTargetType;

import java.math.BigDecimal;
import java.time.Instant;

public record PromotionResponse(
        Long id, String name, PromotionTargetType targetType, Long productId, String productName,
        Long variantId, String variantSku, Long variantProductId, Long categoryId, String categoryName, BigDecimal discountPercent,
        Instant startsAt, Instant endsAt, boolean active, Instant createdAt, Instant updatedAt
) {
    public static PromotionResponse from(Promotion promotion) {
        return new PromotionResponse(promotion.getId(), promotion.getName(), promotion.getTargetType(),
                promotion.getProduct() == null ? null : promotion.getProduct().getId(),
                promotion.getProduct() == null ? null : promotion.getProduct().getName(),
                promotion.getVariant() == null ? null : promotion.getVariant().getId(),
                promotion.getVariant() == null ? null : promotion.getVariant().getSku(),
                promotion.getVariant() == null || promotion.getVariant().getProduct() == null ? null : promotion.getVariant().getProduct().getId(),
                promotion.getCategory() == null ? null : promotion.getCategory().getId(),
                promotion.getCategory() == null ? null : promotion.getCategory().getName(),
                promotion.getDiscountPercent(), promotion.getStartsAt(), promotion.getEndsAt(), promotion.isActive(),
                promotion.getCreatedAt(), promotion.getUpdatedAt());
    }
}

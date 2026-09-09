package com.techstore.dto.response;

import com.techstore.entity.Review;
import com.techstore.enums.ReviewStatus;

import java.time.Instant;

public record ReviewResponse(
        Long id,
        Long productId,
        Long userId,
        String userFullName,
        Integer rating,
        String comment,
        ReviewStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static ReviewResponse from(Review review) {
        return new ReviewResponse(
                review.getId(),
                review.getProduct().getId(),
                review.getUser().getId(),
                review.getUser().getFullName(),
                review.getRating(),
                review.getComment(),
                review.getStatus(),
                review.getCreatedAt(),
                review.getUpdatedAt()
        );
    }
}


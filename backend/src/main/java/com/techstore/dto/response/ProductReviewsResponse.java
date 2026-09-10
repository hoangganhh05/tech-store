package com.techstore.dto.response;

/**
 * Public review summary and the newest approved reviews for a product.
 */
public record ProductReviewsResponse(
        Double averageRating,
        long totalReviews,
        PageResponse<ReviewResponse> reviews
) {
}

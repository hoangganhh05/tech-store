package com.techstore.service;

import com.techstore.dto.request.SubmitReviewRequest;
import com.techstore.dto.response.ReviewEligibilityResponse;
import com.techstore.dto.response.ProductReviewsResponse;
import com.techstore.dto.response.ReviewResponse;

import java.util.Optional;

public interface ReviewService {

    ProductReviewsResponse getProductReviews(Long productId, int page, int size);

    ReviewResponse submitReview(Long userId, Long productId, SubmitReviewRequest request);

    ReviewEligibilityResponse checkEligibility(Long userId, Long productId);

    Optional<ReviewResponse> getMyReview(Long userId, Long productId);
}


package com.techstore.dto.response;

public record ReviewEligibilityResponse(
        boolean canReview,
        ReviewResponse myReview
) {
}


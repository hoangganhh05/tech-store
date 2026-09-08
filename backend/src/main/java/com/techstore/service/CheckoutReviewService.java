package com.techstore.service;

import com.techstore.dto.request.CheckoutReviewRequest;
import com.techstore.dto.response.CheckoutReviewResponse;

public interface CheckoutReviewService {
    CheckoutReviewResponse review(Long userId, CheckoutReviewRequest request);
}

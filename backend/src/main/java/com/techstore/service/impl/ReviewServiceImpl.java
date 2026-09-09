package com.techstore.service.impl;

import com.techstore.dto.request.SubmitReviewRequest;
import com.techstore.dto.response.ReviewEligibilityResponse;
import com.techstore.dto.response.ReviewResponse;
import com.techstore.entity.Product;
import com.techstore.entity.Review;
import com.techstore.entity.User;
import com.techstore.enums.ErrorCode;
import com.techstore.exception.BusinessException;
import com.techstore.repository.ProductRepository;
import com.techstore.repository.ReviewRepository;
import com.techstore.repository.UserRepository;
import com.techstore.service.ReviewService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;

@Service
@Transactional
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    public ReviewServiceImpl(
            ReviewRepository reviewRepository,
            UserRepository userRepository,
            ProductRepository productRepository
    ) {
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
    }

    @Override
    public ReviewResponse submitReview(Long userId, Long productId, SubmitReviewRequest request) {
        Objects.requireNonNull(userId, "UserId cannot be null");
        Objects.requireNonNull(productId, "ProductId cannot be null");
        Objects.requireNonNull(request, "Request cannot be null");

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy người dùng"));

        Product product = productRepository.findByIdAndIsDeletedFalse(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "Không tìm thấy sản phẩm"));

        boolean hasCompleted = reviewRepository.hasCompletedPurchase(userId, productId);
        if (!hasCompleted) {
            throw new BusinessException(
                    ErrorCode.REVIEW_NOT_ELIGIBLE,
                    "Chỉ khách hàng có đơn hàng đã hoàn thành chứa sản phẩm này mới được đánh giá"
            );
        }

        Optional<Review> existingOpt = reviewRepository.findByUserIdAndProductId(userId, productId);
        Review review;
        if (existingOpt.isPresent()) {
            review = existingOpt.get();
            review.setRating(request.rating());
            review.setComment(request.comment());
        } else {
            review = new Review(user, product, request.rating(), request.comment());
        }

        Review saved = reviewRepository.save(review);
        return ReviewResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewEligibilityResponse checkEligibility(Long userId, Long productId) {
        Objects.requireNonNull(userId, "UserId cannot be null");
        Objects.requireNonNull(productId, "ProductId cannot be null");

        boolean canReview = reviewRepository.hasCompletedPurchase(userId, productId);
        Optional<Review> myReview = reviewRepository.findByUserIdAndProductId(userId, productId);

        return new ReviewEligibilityResponse(canReview, myReview.map(ReviewResponse::from).orElse(null));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ReviewResponse> getMyReview(Long userId, Long productId) {
        Objects.requireNonNull(userId, "UserId cannot be null");
        Objects.requireNonNull(productId, "ProductId cannot be null");

        return reviewRepository.findByUserIdAndProductId(userId, productId).map(ReviewResponse::from);
    }
}


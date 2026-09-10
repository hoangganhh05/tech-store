package com.techstore.service.impl;

import com.techstore.dto.request.SubmitReviewRequest;
import com.techstore.dto.response.ReviewEligibilityResponse;
import com.techstore.dto.response.PageResponse;
import com.techstore.dto.response.ProductReviewsResponse;
import com.techstore.dto.response.ReviewResponse;
import com.techstore.entity.Product;
import com.techstore.entity.Review;
import com.techstore.entity.User;
import com.techstore.enums.ErrorCode;
import com.techstore.enums.ReviewStatus;
import com.techstore.exception.BusinessException;
import com.techstore.repository.ProductRepository;
import com.techstore.repository.ReviewRepository;
import com.techstore.repository.UserRepository;
import com.techstore.service.ReviewService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.Objects;
import java.util.Optional;

@Service
@Transactional
public class ReviewServiceImpl implements ReviewService {

    private static final int MAX_REVIEW_PAGE_SIZE = 50;

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
    @Transactional(readOnly = true)
    public ProductReviewsResponse getProductReviews(Long productId, int page, int size) {
        if (productId == null || productId < 1) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Mã sản phẩm không hợp lệ");
        }
        if (page < 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Số trang phải lớn hơn hoặc bằng 0");
        }
        if (size < 1 || size > MAX_REVIEW_PAGE_SIZE) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Kích thước trang phải nằm trong khoảng từ 1 đến " + MAX_REVIEW_PAGE_SIZE);
        }

        productRepository.findByIdAndIsDeletedFalse(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "Không tìm thấy sản phẩm"));

        ReviewStatus approved = ReviewStatus.APPROVED;
        Page<Review> reviewPage = reviewRepository.findByProductIdAndStatus(
                productId,
                approved,
                PageRequest.of(page, size, Sort.by(
                        Sort.Order.desc("createdAt"),
                        Sort.Order.desc("id")
                ))
        );
        Double average = reviewRepository.findAverageRatingByProductIdAndStatus(productId, approved);
        long totalReviews = reviewRepository.countByProductIdAndStatus(productId, approved);

        return new ProductReviewsResponse(
                average == null ? 0.0 : average,
                totalReviews,
                PageResponse.of(reviewPage.map(ReviewResponse::from))
        );
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

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> getAdminReviews(ReviewStatus status, int page, int size) {
        validatePage(page, size);
        Sort sort = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
        Page<Review> reviewPage = status == null
                ? reviewRepository.findAll(PageRequest.of(page, size, sort))
                : reviewRepository.findByStatus(status, PageRequest.of(page, size, sort));
        return PageResponse.of(reviewPage.map(ReviewResponse::from));
    }

    @Override
    public ReviewResponse updateReviewStatus(Long adminUserId, Long reviewId, ReviewStatus status) {
        if (adminUserId == null) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "Không xác định được tài khoản quản trị");
        }
        if (reviewId == null || reviewId < 1) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Mã đánh giá không hợp lệ");
        }
        if (status == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Trạng thái đánh giá không được để trống");
        }

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REVIEW_NOT_FOUND, "Không tìm thấy đánh giá"));
        review.setStatus(status);
        return ReviewResponse.from(reviewRepository.save(review));
    }

    private void validatePage(int page, int size) {
        if (page < 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Số trang phải lớn hơn hoặc bằng 0");
        }
        if (size < 1 || size > MAX_REVIEW_PAGE_SIZE) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Kích thước trang phải nằm trong khoảng từ 1 đến " + MAX_REVIEW_PAGE_SIZE);
        }
    }
}


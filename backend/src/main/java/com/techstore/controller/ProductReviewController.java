package com.techstore.controller;

import com.techstore.dto.request.SubmitReviewRequest;
import com.techstore.dto.response.ApiResponse;
import com.techstore.dto.response.ReviewEligibilityResponse;
import com.techstore.dto.response.ProductReviewsResponse;
import com.techstore.dto.response.ReviewResponse;
import com.techstore.enums.RoleCode;
import com.techstore.security.RequireRole;
import com.techstore.security.RoleAuthorizationInterceptor;
import com.techstore.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${app.api.base-path}/products/{productId}/reviews")
@Validated
@Tag(name = "Product Reviews", description = "Đánh giá và nhận xét sản phẩm")
public class ProductReviewController {

    private final ReviewService reviewService;

    public ProductReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping
    @Operation(summary = "Xem điểm trung bình và danh sách nhận xét đã duyệt của sản phẩm")
    public ApiResponse<ProductReviewsResponse> getProductReviews(
            @PathVariable @Positive(message = "Mã sản phẩm không hợp lệ") Long productId,
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "Số trang phải lớn hơn hoặc bằng 0") int page,
            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "Kích thước trang tối thiểu là 1")
            @Max(value = 50, message = "Kích thước trang tối đa là 50") int size
    ) {
        return ApiResponse.success("Lấy đánh giá sản phẩm thành công",
                reviewService.getProductReviews(productId, page, size));
    }

    @PostMapping
    @RequireRole({RoleCode.CUSTOMER, RoleCode.ADMIN})
    @Operation(summary = "Đánh giá (số sao) và nhận xét cho sản phẩm đã mua")
    public ApiResponse<ReviewResponse> submitReview(
            @RequestAttribute(RoleAuthorizationInterceptor.CURRENT_USER_ID_ATTRIBUTE) Long userId,
            @PathVariable Long productId,
            @Valid @RequestBody SubmitReviewRequest request
    ) {
        ReviewResponse response = reviewService.submitReview(userId, productId, request);
        return ApiResponse.success("Đánh giá sản phẩm thành công", response);
    }

    @GetMapping("/my-review")
    @RequireRole({RoleCode.CUSTOMER, RoleCode.ADMIN})
    @Operation(summary = "Kiểm tra điều kiện đánh giá và lấy đánh giá của người dùng hiện tại")
    public ApiResponse<ReviewEligibilityResponse> getMyReview(
            @RequestAttribute(RoleAuthorizationInterceptor.CURRENT_USER_ID_ATTRIBUTE) Long userId,
            @PathVariable Long productId
    ) {
        ReviewEligibilityResponse response = reviewService.checkEligibility(userId, productId);
        return ApiResponse.success("Lấy thông tin đánh giá thành công", response);
    }
}


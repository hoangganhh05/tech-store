package com.techstore.controller.admin;

import com.techstore.dto.request.UpdateReviewStatusRequest;
import com.techstore.dto.response.ApiResponse;
import com.techstore.dto.response.PageResponse;
import com.techstore.dto.response.ReviewResponse;
import com.techstore.enums.ReviewStatus;
import com.techstore.enums.RoleCode;
import com.techstore.security.RequireRole;
import com.techstore.security.RoleAuthorizationInterceptor;
import com.techstore.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${app.api.base-path}/admin/reviews")
@RequireRole(RoleCode.ADMIN)
@Validated
@Tag(name = "Admin Review Moderation", description = "Duyệt và ẩn đánh giá sản phẩm")
public class AdminReviewController {

    private final ReviewService reviewService;

    public AdminReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping
    @Operation(summary = "Lấy danh sách đánh giá để quản trị")
    public ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> getReviews(
            @RequestParam(required = false) ReviewStatus status,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "Số trang phải lớn hơn hoặc bằng 0") int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "Kích thước trang tối thiểu là 1")
            @Max(value = 50, message = "Kích thước trang tối đa là 50") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách đánh giá thành công",
                reviewService.getAdminReviews(status, page, size)));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Cập nhật trạng thái hiển thị đánh giá")
    public ResponseEntity<ApiResponse<ReviewResponse>> updateReviewStatus(
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateReviewStatusRequest request,
            HttpServletRequest httpServletRequest
    ) {
        Long adminId = (Long) httpServletRequest.getAttribute(RoleAuthorizationInterceptor.CURRENT_USER_ID_ATTRIBUTE);
        ReviewResponse response = reviewService.updateReviewStatus(adminId, id, request.status());
        String message = switch (request.status()) {
            case APPROVED -> "Duyệt đánh giá thành công";
            case HIDDEN -> "Ẩn đánh giá thành công";
            case PENDING -> "Đưa đánh giá về trạng thái chờ duyệt thành công";
        };
        return ResponseEntity.ok(ApiResponse.success(message, response));
    }
}

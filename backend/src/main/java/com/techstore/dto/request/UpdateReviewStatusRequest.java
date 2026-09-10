package com.techstore.dto.request;

import com.techstore.enums.ReviewStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateReviewStatusRequest(
        @NotNull(message = "Trạng thái đánh giá không được để trống")
        ReviewStatus status
) {
}

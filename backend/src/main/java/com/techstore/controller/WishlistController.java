package com.techstore.controller;

import com.techstore.dto.response.ApiResponse;
import com.techstore.dto.response.PageResponse;
import com.techstore.dto.response.StorefrontProductResponse;
import com.techstore.dto.response.WishlistItemResponse;
import com.techstore.enums.RoleCode;
import com.techstore.security.RequireRole;
import com.techstore.security.RoleAuthorizationInterceptor;
import com.techstore.service.WishlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${app.api.base-path}/wishlist")
@Validated
@RequireRole(RoleCode.CUSTOMER)
@Tag(name = "Wishlist", description = "Danh sách sản phẩm yêu thích của khách hàng")
public class WishlistController {

    private final WishlistService wishlistService;

    public WishlistController(WishlistService wishlistService) {
        this.wishlistService = wishlistService;
    }

    @GetMapping
    @Operation(summary = "Lấy danh sách sản phẩm yêu thích của khách hàng hiện tại")
    public ApiResponse<PageResponse<StorefrontProductResponse>> getWishlist(
            @RequestAttribute(RoleAuthorizationInterceptor.CURRENT_USER_ID_ATTRIBUTE) Long userId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "12") @Min(1) @Max(100) int size
    ) {
        return ApiResponse.success("Lấy danh sách yêu thích thành công",
                wishlistService.getWishlist(userId, page, size));
    }

    @PostMapping("/{productId}")
    @Operation(summary = "Thêm sản phẩm vào danh sách yêu thích")
    public ApiResponse<WishlistItemResponse> addToWishlist(
            @RequestAttribute(RoleAuthorizationInterceptor.CURRENT_USER_ID_ATTRIBUTE) Long userId,
            @PathVariable @Positive(message = "Mã sản phẩm không hợp lệ") Long productId
    ) {
        return ApiResponse.success("Đã thêm sản phẩm vào danh sách yêu thích",
                wishlistService.addToWishlist(userId, productId));
    }

    @DeleteMapping("/{productId}")
    @Operation(summary = "Xoá sản phẩm khỏi danh sách yêu thích")
    public ApiResponse<WishlistItemResponse> removeFromWishlist(
            @RequestAttribute(RoleAuthorizationInterceptor.CURRENT_USER_ID_ATTRIBUTE) Long userId,
            @PathVariable @Positive(message = "Mã sản phẩm không hợp lệ") Long productId
    ) {
        return ApiResponse.success("Đã xoá sản phẩm khỏi danh sách yêu thích",
                wishlistService.removeFromWishlist(userId, productId));
    }
}

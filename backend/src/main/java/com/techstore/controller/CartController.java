package com.techstore.controller;

import com.techstore.dto.request.AddToCartRequest;
import com.techstore.dto.request.SyncCartRequest;
import com.techstore.dto.request.UpdateCartItemRequest;
import com.techstore.dto.response.ApiResponse;
import com.techstore.dto.response.CartResponse;
import com.techstore.dto.response.CartSyncResponse;
import com.techstore.dto.response.CartValidationResponse;
import com.techstore.enums.ErrorCode;
import com.techstore.exception.BusinessException;
import com.techstore.security.AccessTokenAuthenticator;
import com.techstore.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${app.api.base-path}/cart")
@Tag(name = "Cart", description = "Shopping cart endpoints")
public class CartController {

    private final CartService cartService;
    private final AccessTokenAuthenticator accessTokenAuthenticator;

    public CartController(CartService cartService, AccessTokenAuthenticator accessTokenAuthenticator) {
        this.cartService = cartService;
        this.accessTokenAuthenticator = accessTokenAuthenticator;
    }

    @PostMapping("/items")
    @Operation(summary = "Thêm biến thể sản phẩm vào giỏ hàng (hỗ trợ cả người dùng đăng nhập và khách vãng lai)")
    public ResponseEntity<ApiResponse<CartResponse>> addToCart(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId,
            @Valid @RequestBody AddToCartRequest request
    ) {
        Long userId = resolveUserId(authorizationHeader);
        CartResponse response = cartService.addToCart(userId, sessionId, request);
        return ResponseEntity.ok(ApiResponse.success("Thêm sản phẩm vào giỏ hàng thành công", response));
    }

    @GetMapping
    @Operation(summary = "Lấy thông tin chi tiết giỏ hàng hiện tại")
    public ResponseEntity<ApiResponse<CartResponse>> getCart(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId
    ) {
        Long userId = resolveUserId(authorizationHeader);
        CartResponse response = cartService.getCart(userId, sessionId);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin giỏ hàng thành công", response));
    }

    @PatchMapping("/items/{id}")
    @Operation(summary = "Cập nhật số lượng của dòng sản phẩm trong giỏ hàng")
    public ResponseEntity<ApiResponse<CartResponse>> updateCartItemQuantity(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId,
            @PathVariable("id") Long itemId,
            @Valid @RequestBody UpdateCartItemRequest request
    ) {
        Long userId = resolveUserId(authorizationHeader);
        CartResponse response = cartService.updateCartItemQuantity(userId, sessionId, itemId, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật số lượng giỏ hàng thành công", response));
    }

    @DeleteMapping("/items/{id}")
    @Operation(summary = "Xoá dòng sản phẩm khỏi giỏ hàng")
    public ResponseEntity<ApiResponse<CartResponse>> removeCartItem(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId,
            @PathVariable("id") Long itemId
    ) {
        Long userId = resolveUserId(authorizationHeader);
        CartResponse response = cartService.removeCartItem(userId, sessionId, itemId);
        return ResponseEntity.ok(ApiResponse.success("Xoá sản phẩm khỏi giỏ hàng thành công", response));
    }

    @PostMapping("/validate")
    @Operation(summary = "Kiểm tra lại tồn kho toàn bộ giỏ hàng trước khi checkout")
    public ResponseEntity<ApiResponse<CartValidationResponse>> validateCartStock(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId
    ) {
        Long userId = resolveUserId(authorizationHeader);
        CartValidationResponse response = cartService.validateCartStock(userId, sessionId);
        return ResponseEntity.ok(ApiResponse.success("Kiểm tra tồn kho giỏ hàng thành công", response));
    }

    @PostMapping("/sync")
    @Operation(summary = "Đồng bộ/gộp giỏ hàng tạm thời của khách vãng lai vào tài khoản sau khi đăng nhập")
    public ResponseEntity<ApiResponse<CartSyncResponse>> syncCart(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @RequestHeader(value = "X-Session-Id", required = false) String sessionIdHeader,
            @RequestBody(required = false) SyncCartRequest request
    ) {
        Long userId = accessTokenAuthenticator.authenticate(authorizationHeader);
        String sessionId = (request != null && request.sessionId() != null && !request.sessionId().isBlank())
                ? request.sessionId()
                : sessionIdHeader;

        CartSyncResponse response = cartService.syncCart(userId, sessionId);
        return ResponseEntity.ok(ApiResponse.success(response.message(), response));
    }

    private Long resolveUserId(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return null;
        }
        return accessTokenAuthenticator.authenticate(authorizationHeader);
    }
}

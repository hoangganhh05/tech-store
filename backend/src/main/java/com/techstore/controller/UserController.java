package com.techstore.controller;

import com.techstore.dto.request.AddressRequest;
import com.techstore.dto.request.ChangePasswordRequest;
import com.techstore.dto.request.UpdateProfileRequest;
import com.techstore.dto.response.AddressResponse;
import com.techstore.dto.response.ApiResponse;
import com.techstore.dto.response.UserProfileResponse;
import com.techstore.security.AccessTokenAuthenticator;
import com.techstore.service.AccountPasswordService;
import com.techstore.service.AddressService;
import com.techstore.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("${app.api.base-path}/users")
@Tag(name = "Users", description = "Authenticated user profile endpoints")
public class UserController {

    private final AccessTokenAuthenticator accessTokenAuthenticator;
    private final UserProfileService userProfileService;
    private final AccountPasswordService accountPasswordService;
    private final AddressService addressService;

    public UserController(
            AccessTokenAuthenticator accessTokenAuthenticator,
            UserProfileService userProfileService,
            AccountPasswordService accountPasswordService,
            AddressService addressService
    ) {
        this.accessTokenAuthenticator = accessTokenAuthenticator;
        this.userProfileService = userProfileService;
        this.accountPasswordService = accountPasswordService;
        this.addressService = addressService;
    }

    // ─── Profile ──────────────────────────────────────────────────────────────

    @GetMapping("/me")
    @Operation(summary = "Get the authenticated user's profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getMyProfile(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader
    ) {
        Long userId = accessTokenAuthenticator.authenticate(authorizationHeader);
        return ResponseEntity.ok(ApiResponse.success(userProfileService.getProfile(userId)));
    }

    @PutMapping("/me")
    @Operation(summary = "Update the authenticated user's profile without changing email")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateMyProfile(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        Long userId = accessTokenAuthenticator.authenticate(authorizationHeader);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật thông tin cá nhân thành công", userProfileService.updateProfile(userId, request)));
    }

    @PutMapping("/me/password")
    @Operation(summary = "Change the authenticated user's password")
    public ResponseEntity<ApiResponse<Void>> changeMyPassword(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        Long userId = accessTokenAuthenticator.authenticate(authorizationHeader);
        accountPasswordService.changePassword(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Đổi mật khẩu thành công", null));
    }

    // ─── Addresses ────────────────────────────────────────────────────────────

    @GetMapping("/me/addresses")
    @Operation(summary = "List all shipping addresses for the authenticated user")
    public ResponseEntity<ApiResponse<List<AddressResponse>>> listMyAddresses(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader
    ) {
        Long userId = accessTokenAuthenticator.authenticate(authorizationHeader);
        return ResponseEntity.ok(ApiResponse.success(addressService.listAddresses(userId)));
    }

    @PostMapping("/me/addresses")
    @Operation(summary = "Add a new shipping address")
    public ResponseEntity<ApiResponse<AddressResponse>> addMyAddress(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @Valid @RequestBody AddressRequest request
    ) {
        Long userId = accessTokenAuthenticator.authenticate(authorizationHeader);
        AddressResponse created = addressService.addAddress(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Thêm địa chỉ giao hàng thành công", created));
    }

    @PutMapping("/me/addresses/{id}")
    @Operation(summary = "Update an existing shipping address")
    public ResponseEntity<ApiResponse<AddressResponse>> updateMyAddress(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @PathVariable Long id,
            @Valid @RequestBody AddressRequest request
    ) {
        Long userId = accessTokenAuthenticator.authenticate(authorizationHeader);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật địa chỉ giao hàng thành công",
                addressService.updateAddress(userId, id, request)));
    }

    @DeleteMapping("/me/addresses/{id}")
    @Operation(summary = "Delete a shipping address")
    public ResponseEntity<ApiResponse<Void>> deleteMyAddress(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @PathVariable Long id
    ) {
        Long userId = accessTokenAuthenticator.authenticate(authorizationHeader);
        addressService.deleteAddress(userId, id);
        return ResponseEntity.ok(ApiResponse.success("Xoá địa chỉ giao hàng thành công", null));
    }

    @PatchMapping("/me/addresses/{id}/default")
    @Operation(summary = "Set a shipping address as the default")
    public ResponseEntity<ApiResponse<AddressResponse>> setMyDefaultAddress(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @PathVariable Long id
    ) {
        Long userId = accessTokenAuthenticator.authenticate(authorizationHeader);
        return ResponseEntity.ok(ApiResponse.success("Đặt địa chỉ mặc định thành công",
                addressService.setDefaultAddress(userId, id)));
    }
}


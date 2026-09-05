package com.techstore.controller;

import com.techstore.dto.request.ChangePasswordRequest;
import com.techstore.dto.request.UpdateProfileRequest;
import com.techstore.dto.response.ApiResponse;
import com.techstore.dto.response.UserProfileResponse;
import com.techstore.security.AccessTokenAuthenticator;
import com.techstore.service.AccountPasswordService;
import com.techstore.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${app.api.base-path}/users")
@Tag(name = "Users", description = "Authenticated user profile endpoints")
public class UserController {

    private final AccessTokenAuthenticator accessTokenAuthenticator;
    private final UserProfileService userProfileService;
    private final AccountPasswordService accountPasswordService;

    public UserController(
            AccessTokenAuthenticator accessTokenAuthenticator,
            UserProfileService userProfileService,
            AccountPasswordService accountPasswordService
    ) {
        this.accessTokenAuthenticator = accessTokenAuthenticator;
        this.userProfileService = userProfileService;
        this.accountPasswordService = accountPasswordService;
    }

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
}

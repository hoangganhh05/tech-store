package com.techstore.controller.admin;

import com.techstore.dto.response.ApiResponse;
import com.techstore.enums.RoleCode;
import com.techstore.security.AccessTokenClaims;
import com.techstore.security.RequireRole;
import com.techstore.security.RoleAuthorizationInterceptor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("${app.api.base-path}/admin")
@RequireRole(RoleCode.ADMIN)
@Tag(name = "Admin Probe", description = "Admin probe and health check endpoints protected by ADMIN role")
public class AdminProbeController {

    @GetMapping("/probe")
    @Operation(summary = "Check admin authorization")
    public ResponseEntity<ApiResponse<Map<String, Object>>> probe(HttpServletRequest request) {
        AccessTokenClaims claims = (AccessTokenClaims) request.getAttribute(RoleAuthorizationInterceptor.CURRENT_USER_CLAIMS_ATTRIBUTE);
        Map<String, Object> data = Map.of(
                "authorized", true,
                "role", RoleCode.ADMIN.name(),
                "email", claims != null ? claims.email() : "",
                "userId", claims != null ? claims.userId() : 0L
        );
        return ResponseEntity.ok(ApiResponse.success("Truy cập khu vực quản trị thành công", data));
    }
}

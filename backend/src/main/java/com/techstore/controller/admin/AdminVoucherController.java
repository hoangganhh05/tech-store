package com.techstore.controller.admin;

import com.techstore.dto.request.VoucherRequest;
import com.techstore.dto.response.ApiResponse;
import com.techstore.dto.response.PageResponse;
import com.techstore.dto.response.VoucherResponse;
import com.techstore.enums.RoleCode;
import com.techstore.security.RequireRole;
import com.techstore.service.VoucherManagementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${app.api.base-path}/admin/vouchers")
@RequireRole(RoleCode.ADMIN)
public class AdminVoucherController {
    private final VoucherManagementService voucherManagementService;

    public AdminVoucherController(VoucherManagementService voucherManagementService) {
        this.voucherManagementService = voucherManagementService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<VoucherResponse>> create(@Valid @RequestBody VoucherRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Tạo voucher thành công", voucherManagementService.create(request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<VoucherResponse>>> getAll(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách voucher thành công", voucherManagementService.getAll(keyword, page, size)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<VoucherResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết voucher thành công", voucherManagementService.getById(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<VoucherResponse>> update(@PathVariable Long id, @Valid @RequestBody VoucherRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Cập nhật voucher thành công", voucherManagementService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        voucherManagementService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Xoá voucher thành công", null));
    }
}
